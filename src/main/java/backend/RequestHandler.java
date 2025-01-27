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

import common.RubusSocket;
import common.net.request.RubusRequestType;

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
		byte[] buffer = new byte[1024];
		int actualBufferSize = 0;
		int byteRead;
		do {
			byteRead = socket.read(buffer);
			actualBufferSize += byteRead;
			if (actualBufferSize == buffer.length) {
				buffer = Arrays.copyOf(buffer, buffer.length * 2);
			}
		} while (byteRead > 0);
		if (actualBufferSize == 0) return;

		String request = new String(buffer);
		try {
			RubusRequestType requestType =
				RubusRequestType.valueOf(request.substring(request.indexOf("request-type "), request.indexOf('\n')));
			switch (requestType) {
				case LIST -> {
					String titlePattern = request.substring(
						request.indexOf("title-contains ") + "title-contains ".length(),
						request.indexOf('\n', request.indexOf("title-contains "))
					);
				}
				case INFO -> {
					String mediaID = request.substring(
						request.indexOf("media-id ") + "media-id".length(),
						request.indexOf('\n', request.indexOf("media-id "))
					);

				}
				case FETCH -> {
					String mediaID = request.substring(
						request.indexOf("media-id ") + "media-id".length(),
						request.indexOf('\n', request.indexOf("media-id "))
					);
					long beginningPieceIndex = Long.parseLong(
						request.substring(
							request.indexOf("first-playback-piece ") + "first-playback-piece ".length(),
							request.indexOf('\n', request.indexOf("first-playback-piece "))
						)
					);
					long piecesToFetch = Long.parseLong(
						request.substring(
							request.indexOf("number-playback-pieces ") + "number-playback-pieces ".length(),
							request.indexOf('\n', request.indexOf("number-playback-pieces "))
						)
					);
				}
			}
		} catch (IndexOutOfBoundsException indexOutOfBoundsException) {

		} catch (IllegalArgumentException illegalArgumentException) {

		}

		consumer.accept(socket);
	}

	public RubusSocket getSocket() {
		return socket;
	}
}