/*
 * Rubus is an application level protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024-2025 Yegore Vlussove
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package backend;

import backend.io.Media;
import backend.io.MediaPool;
import common.RubusSocket;
import common.net.request.RubusRequestType;
import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;
import common.net.response.body.PlaybackInfo;
import common.net.response.body.PlaybackList;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * RequestHandler is responsible for handling clients' requests and sending appropriate responses. It also detects
 * if the client closed its session, but it doesn't close the socket on its on and instead allows the caller to handle
 * it. If RequestHandler didn't receive a client's request it assumes that the client decided to keep the connection.
 */
public class RequestHandler implements Runnable {

	private final RubusSocket socket;

	private final Consumer<RubusSocket> keepConnection;

	private final Consumer<RubusSocket> closeConnection;

	/**
	 * Creates a new instance of this class.
	 * @param socket socket the request of which needs to be handled
	 * @param keepConnection calls if the client kept the connection
	 * @param closeConnection calls if the client closed the connection
	 */
	public RequestHandler(RubusSocket socket, Consumer<RubusSocket> keepConnection, Consumer<RubusSocket> closeConnection) {
		assert socket != null;

		this.keepConnection = keepConnection;
		this.closeConnection = closeConnection;
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			byte[] request;
			try {
				 request = retrieveRequest(socket);
			} catch (SocketTimeoutException ignored) {
				keepConnection.accept(socket);
				return;
			} catch (EOFException e) {
				closeConnection.accept(socket);
				return;
			}

			String requestMes = new String(request);
			StringBuilder responseMes = new StringBuilder("response-type ").append(RubusResponseType.OK).append('\n');
			ByteArrayOutputStream body = new ByteArrayOutputStream();
			RubusRequestType requestType =
				RubusRequestType.valueOf(requestMes.substring(requestMes.indexOf(' ') + 1, requestMes.indexOf('\n')));
			switch (requestType) {
				case LIST -> {
					String titlePattern = requestMes.substring(
						requestMes.indexOf("title-contains ") + "title-contains ".length(),
						requestMes.indexOf('\n', requestMes.indexOf("title-contains "))
					);
					ArrayList<String> ids = new ArrayList<>();
					ArrayList<String> titles = new ArrayList<>();
					for (Media m: MediaPool.availableMediaFast()) {
						if (m.getTitle().matches(titlePattern)) {
							ids.add(m.getID());
							titles.add(m.getTitle());
						}
					}
					PlaybackList playbackList = new PlaybackList(ids.toArray(new String[0]), titles.toArray(new String[0]));
					responseMes.append("serialized-object ").append(PlaybackList.class.getName()).append('\n');
					ObjectOutputStream oos = new ObjectOutputStream(body);
					oos.writeObject(playbackList);
				}

				case INFO -> {
					String mediaID = requestMes.substring(
						requestMes.indexOf("media-id ") + "media-id ".length(),
						requestMes.indexOf('\n', requestMes.indexOf("media-id "))
					);
					Media media = MediaPool.getMedia(mediaID);
					PlaybackInfo playbackInfo = media.toPlaybackInfo();
					responseMes.append("serialized-object ").append(PlaybackInfo.class.getName()).append('\n');
					ObjectOutputStream oos = new ObjectOutputStream(body);
					oos.writeObject(playbackInfo);
				}

				case FETCH -> {
					String mediaID = requestMes.substring(
						requestMes.indexOf("media-id ") + "media-id ".length(),
						requestMes.indexOf('\n', requestMes.indexOf("media-id "))
					);
					int beginningPieceIndex = Integer.parseInt(
						requestMes.substring(
							requestMes.indexOf("first-playback-piece ") + "first-playback-piece ".length(),
							requestMes.indexOf('\n', requestMes.indexOf("first-playback-piece "))
						)
					);
					int piecesToFetch = Integer.parseInt(
						requestMes.substring(
							requestMes.indexOf("number-playback-pieces ") + "number-playback-pieces ".length(),
							requestMes.indexOf('\n', requestMes.indexOf("number-playback-pieces "))
						)
					);
					Media media = MediaPool.getMedia(mediaID);
					FetchedPieces fetchedPieces =
						new FetchedPieces(
							mediaID,
							beginningPieceIndex,
							media.fetchVideoPieces(beginningPieceIndex, piecesToFetch),
							media.fetchAudioPieces(beginningPieceIndex, piecesToFetch)
						);
					responseMes.append("serialized-object ").append(FetchedPieces.class.getName()).append('\n');
					ObjectOutputStream oos = new ObjectOutputStream(body);
					oos.writeObject(fetchedPieces);
				}
			}
			responseMes.append("body-length ").append(body.size()).append("\n\n");
			byte[] response = Arrays.copyOf(responseMes.toString().getBytes(), responseMes.length() + body.size());
			System.arraycopy(body.toByteArray(), 0, response, responseMes.length(), body.size());
			socket.write(response);
		} catch (IndexOutOfBoundsException indexOutOfBoundsException) {
			try {
				String errorMsg =
					"response-type " + RubusResponseType.BAD_PARAMETERS + "\n" +
					"body-length 0\n\n";
				socket.write(errorMsg.getBytes());
			} catch (IOException ignored) {}
		} catch (IllegalArgumentException illegalArgumentException) {
			try {
				String errorMsg =
					"response-type " + RubusResponseType.BAD_REQUEST + "\n" +
					"body-length 0\n\n";
				socket.write(errorMsg.getBytes());
			} catch (IOException ignored) {}
		} catch (IOException e) {
			try {
				String errorMsg =
					"response-type " + RubusResponseType.SERVER_ERROR + "\n" +
					"body-length 0\n\n";
				socket.write(errorMsg.getBytes());
			} catch (IOException ignored) {}
		}

		keepConnection.accept(socket);
	}

	public RubusSocket getSocket() {
		return socket;
	}

	private byte[] retrieveRequest(RubusSocket socket) throws IOException {
		int maxHeaderAllocation = 1024 * 8;
		byte[] request = new byte[1024];
		int emptyLineIndex;
		int byteReadTotal = 0;
		while ((emptyLineIndex = searchSubArray(request, "\n\n".getBytes())) == -1) {
			if (request.length > maxHeaderAllocation) throw new IllegalArgumentException();
			if (byteReadTotal == request.length)
				request = Arrays.copyOf(request, request.length * 2);
			int byteRead = socket.read(request, byteReadTotal, request.length - byteReadTotal, 10);
			if (byteRead == -1) throw new EOFException();
			byteReadTotal += byteRead;
		}

		String header = new String(request, 0, emptyLineIndex + 1);
		int bodyLen = Integer.parseInt(header.substring(
			header.indexOf("body-length ") + "body-length ".length(),
			header.indexOf('\n', header.indexOf("body-length "))
		));

		request = Arrays.copyOf(request, header.length() + "\n".length() + bodyLen);
		do {
			int remaining = request.length - byteReadTotal;
			byteReadTotal += socket.read(request, byteReadTotal, remaining, 2000);
		} while (byteReadTotal < request.length);

		return request;
	}

	private int searchSubArray(byte[] oArr, byte[] sArr) {
		int sArrayByteIndex = 0;
		for (int i = 0; i < oArr.length - sArr.length + 1; i++) {
			if (sArrayByteIndex == sArr.length) return i - sArr.length;
			if (oArr[i] == sArr[sArrayByteIndex]) sArrayByteIndex++;
			else sArrayByteIndex = 0;
		}
		return -1;
	}
}