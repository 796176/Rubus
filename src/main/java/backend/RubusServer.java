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
import common.RubusSockets;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * RubusServer is responsible for accepting or rejecting clients' requests to establish a connection.
 */
public class RubusServer extends Thread {

	private boolean isRunning = true;

	private final int maxConnections;

	private SocketManager manager;

	private final RubusServerSocket serverSocket;

	/**
	 * Constructs an instance of this class.
	 * @param maxConnections the limit of how many connection can be opened at any given moment
	 */
	public RubusServer(int maxConnections) {
		assert maxConnections > 0;

		try {
			serverSocket = RubusSockets.getRubusServerSocket(54300);
		} catch (IOException ioException) {
			throw new RuntimeException(ioException);
		}
		this.maxConnections = maxConnections;
		manager = SocketManager.newSocketManager(maxConnections);
	}

	/**
	 * Constructs an instance of this class using the default limit for opened connections.
	 */
	public RubusServer() {
		this(8);
	}

	@Override
	public void run() {
		while (isRunning) {
			try {
				if (manager.getActiveConnections() < maxConnections) {
					RubusSocket socket = serverSocket.accept(50);
					manager.add(socket);
				}
			} catch (SocketTimeoutException ignored) {}
			catch (IOException ioException) {

			}
		}
	}

	/**
	 * Terminates the server.
	 */
	public void terminate() {
		isRunning = false;
		manager.terminate();
		serverSocket.close();
	}

	/**
	 * Returns true if the server is running, false otherwise.
	 * @return true if the server is running, false otherwise
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Returns the limit of how many connection can be opened at any given moment.
	 * @return the limit of how many connection can be opened at any given moment
	 */
	public int getMaximumOpenedConnections() {
		return maxConnections;
	}

	public static void main(String[] args) throws InterruptedException {
		RubusServer rubusServer = new RubusServer(4);
		rubusServer.start();
		rubusServer.join();
	}
}
