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

package frontend;

import common.RubusSocket;

import java.io.IOException;
import java.util.Arrays;

public class RubusClient {

	private RubusSocket socket;

	public RubusClient(RubusSocket rubusSocket) {
		assert rubusSocket != null;

		socket = rubusSocket;
	}

	public void setSocket(RubusSocket rubusSocket) {
		assert rubusSocket != null;

		socket = rubusSocket;
	}

	public RubusSocket getSocket() {
		return socket;
	}

	public RubusResponse send(RubusRequest request, long timeout) throws IOException {
		assert request != null && timeout > 0;

		socket.write(request.getBytes());
		byte[] response = new byte[1024];
		int read = 0;
		int readAll = 0;
		do {
			read = socket.read(response, read, response.length - read, timeout);
			readAll += read;
			if (readAll == response.length) {
				response = Arrays.copyOf(response, response.length * 2);
			}
		} while (read != 0);
		return new RubusResponse(Arrays.copyOfRange(response, 0, readAll));
	}
}
