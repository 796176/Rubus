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

package frontend;

import common.RubusSocket;
import common.RubusSockets;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

/**
 * RubusClient is an auxiliary class designed to simplify sending rubus requests and receiving rubus responses.
 */
public class RubusClient {

	private RubusSocket socket;

	/**
	 * Constructs this class using a network socket.
	 * @param rubusSocket a network socket
	 */
	public RubusClient(RubusSocket rubusSocket) {
		assert rubusSocket != null;

		socket = rubusSocket;
	}

	/**
	 * Sets a new socket.
	 * @param rubusSocket a new socket
	 */
	public void setSocket(RubusSocket rubusSocket) {
		assert rubusSocket != null;

		socket = rubusSocket;
	}

	/**
	 * Returns the current socket.
	 * @return the current socket
	 */
	public RubusSocket getSocket() {
		return socket;
	}

	/**
	 * Sends the request and waits to receive the server response. If the sending or the receiving is not done withing
	 * the specified time exits by throwing an exception.
	 * @param request a request to send
	 * @param timeout a timeout
	 * @return a server response
	 * @throws IOException if some I/O error occur
	 */
	public RubusResponse send(RubusRequest request, long timeout) throws IOException {
		assert request != null && timeout > 0;

		socket.write(request.getBytes());
		byte[] response = RubusSockets.extractMessage(socket, timeout);
		return new RubusResponse(response);
	}


}

