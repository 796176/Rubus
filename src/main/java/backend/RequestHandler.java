/*
 * Rubus is an application level protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024 Yegore Vlussove
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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class RequestHandler implements Runnable {

	private final RubusSocket socket;

	private final Consumer<RubusSocket> consumer;

	public RequestHandler(RubusSocket socket, Consumer<RubusSocket> releaseSocket) {
		assert socket != null;

		this.consumer = releaseSocket;
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			byte[] request = retrieveRequest(socket);
			if (request.length == 0) return;

			String requestMes = new String(request);
			StringBuilder responseMes = new StringBuilder("response-type ").append(RubusResponseType.OK).append('\n');
			ByteArrayOutputStream body = new ByteArrayOutputStream();
			RubusRequestType requestType =
				RubusRequestType.valueOf(requestMes.substring(requestMes.indexOf("request-type "), requestMes.indexOf('\n')));
			switch (requestType) {
				case LIST -> {
					String titlePattern = requestMes.substring(
						requestMes.indexOf("title-contains ") + "title-contains ".length(),
						requestMes.indexOf('\n', requestMes.indexOf("title-contains "))
					);
					ArrayList<String> ids = new ArrayList<>();
					ArrayList<String> titles = new ArrayList<>();
					Arrays
						.stream(MediaPool.availableMediaFast())
						.filter(media -> media.getTitle().matches(titlePattern))
						.forEach(media -> {
							ids.add(media.getID());
							titles.add(media.getTitle());
						});
					PlaybackList playbackList = new PlaybackList(ids.toArray(new String[0]), titles.toArray(new String[0]));
					responseMes.append("serialized-object ").append(PlaybackList.class.getName()).append('\n');
					ObjectOutputStream oos = new ObjectOutputStream(body);
					oos.writeObject(playbackList);
				}

				case INFO -> {
					String mediaID = requestMes.substring(
						requestMes.indexOf("media-id ") + "media-id".length(),
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
						requestMes.indexOf("media-id ") + "media-id".length(),
						requestMes.indexOf('\n', requestMes.indexOf("media-id "))
					);
					long beginningPieceIndex = Long.parseLong(
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
							media.getVideoEncodingType(),
							media.fetchVideoPieces(beginningPieceIndex, piecesToFetch),
							media.getAudioEncodingType(),
							media.fetchAudioPieces(beginningPieceIndex, piecesToFetch)
						);
					ObjectOutputStream oos = new ObjectOutputStream(body);
					oos.writeObject(fetchedPieces);
				}
			}
			byte[] response = Arrays.copyOf(responseMes.toString().getBytes(), responseMes.length() + body.size());
			System.arraycopy(body, 0, response, responseMes.length(), body.size());
			socket.write(response);
		} catch (IndexOutOfBoundsException indexOutOfBoundsException) {
			try {
				socket.write(("response-type " + RubusResponseType.BAD_PARAMETERS + "\n").getBytes());
			} catch (IOException ignored) {}
		} catch (IllegalArgumentException illegalArgumentException) {
			try {
				socket.write(("response-type " + RubusResponseType.BAD_REQUEST + "\n").getBytes());
			} catch (IOException ignored) {}
		} catch (IOException e) {
			try {
				socket.write(("response-type" + RubusResponseType.SERVER_ERROR + "\n").getBytes());
			} catch (IOException ignored) {}
		}

		consumer.accept(socket);
	}

	public RubusSocket getSocket() {
		return socket;
	}

	private byte[] retrieveRequest(RubusSocket socket) throws IOException {
		byte[] request = new byte[1024];
		int actualBufferSize = 0;
		int byteRead;
		do {
			byteRead = socket.read(request);
			actualBufferSize += byteRead;
			if (actualBufferSize == request.length) {
				request = Arrays.copyOf(request, request.length * 2);
			}
		} while (byteRead > 0);
		return Arrays.copyOf(request, actualBufferSize);
	}
}