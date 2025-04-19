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

import common.RubusSocket;
import common.TCPRubusSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * TCPRubusServerSocket is a concrete implementation of {@link RubusServerSocket} that establishes TCP connections
 * between a server and a client and returns instances of {@link TCPRubusSocket} after the connection is established.
 */
public class TCPRubusServerSocket implements RubusServerSocket {

	private final ServerSocket serverSocket;

	private final int defaultTimeout;

	/**
	 * Constructs an instance of this class.
	 * @param serverAddress the server address this TCPRubusServerSocket is bound to
	 * @param port the listening port
	 * @throws IOException if some I/O error occurs
	 */
	public TCPRubusServerSocket(InetAddress serverAddress, int port) throws IOException {
		serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress(serverAddress, port));
		defaultTimeout = serverSocket.getSoTimeout();
	}

	@Override
	public RubusSocket accept() throws IOException{
		serverSocket.setSoTimeout(defaultTimeout);
		return new TCPRubusSocket(serverSocket.accept(), System.currentTimeMillis());
	}

	@Override
	public RubusSocket accept(long timeout) throws IOException {
		serverSocket.setSoTimeout((int) timeout);
		return new TCPRubusSocket(serverSocket.accept(), System.currentTimeMillis());
	}

	@Override
	public void close() throws IOException {
		serverSocket.close();
	}

	@Override
	public boolean isClosed() {
		return serverSocket.isClosed();
	}
}