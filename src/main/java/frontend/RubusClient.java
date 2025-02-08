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

		int maxHeaderAllocation = 1024 * 8;
		byte[] response = new byte[1024];
		int emptyLineIndex;
		int byteRead = 0;
		while ((emptyLineIndex = searchSubArray(response, "\n\n".getBytes())) == -1) {
			if (byteRead > maxHeaderAllocation) throw new IllegalArgumentException();
			if (byteRead == response.length)
				response = Arrays.copyOf(response, response.length * 2);
			byteRead += socket.read(response, timeout);
		}

		String header = new String(response, 0, emptyLineIndex + 1);
		int bodyLen = Integer.parseInt(header.substring(
			header.indexOf("body-length ") + "body-length ".length(),
			header.indexOf('\n', header.indexOf("body-length "))
		));

		response = Arrays.copyOf(response, header.length() + "\n".length() + bodyLen);
		do {
			int remaining = response.length - byteRead;
			byteRead += socket.read(response, byteRead, remaining, timeout);
		} while (byteRead < response.length);

		return new RubusResponse(response);
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

