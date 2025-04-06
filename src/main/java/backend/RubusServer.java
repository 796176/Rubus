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

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * RubusServer is responsible for accepting or rejecting incoming connections.
 */
public class RubusServer extends Thread {

	private boolean isRunning = true;

	private final int connectionLimit;

	private final SocketManager manager;

	private final RubusServerSocket serverSocket;

	/**
	 * Constructs an instance of this class.
	 * @param socketManager the socket manager
	 * @param rubusServerSocket the concrete implementation of {@link RubusServerSocket}
	 * @param openConnectionLimit the limit of how many connections this RubusServer can keep open at a time
	 */
	public RubusServer(SocketManager socketManager, RubusServerSocket rubusServerSocket, int openConnectionLimit) {
		assert socketManager != null && rubusServerSocket != null && openConnectionLimit > 0;

		serverSocket = rubusServerSocket;
		manager = socketManager;
		this.connectionLimit = openConnectionLimit;
	}

	@Override
	public void run() {
		while (isRunning) {
			try {
				if (manager.getActiveConnections() < connectionLimit) {
					RubusSocket socket = serverSocket.accept(50);
					manager.add(socket);
				}
			} catch (SocketTimeoutException ignored) {}
			catch (IOException ioException) {

			}
		}
	}

	/**
	 * Terminates the server and the underlying socket manager.
	 */
	public void terminate() throws IOException {
		isRunning = false;
		manager.terminate();
		serverSocket.close();
	}

	/**
	 * Returns true if this RubusServer is running, false otherwise.
	 * @return true if this RubusServer is running, false otherwise
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Returns the limit of how many connections this RubusServer can keep open at a time.
	 * @return the limit of how many connections this RubusServer can keep open at a time
	 */
	public int getOpenConnectionsLimit() {
		return connectionLimit;
	}
}
