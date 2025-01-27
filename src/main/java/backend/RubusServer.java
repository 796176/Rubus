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

import java.net.SocketTimeoutException;

public class RubusServer extends Thread {

	private boolean isRunning = true;

	private final int maxConnections;

	private SocketManager manager;

	private final RubusServerSocket serverSocket = RubusServerSocketFactory.get();

	public RubusServer(int maxConnections) {
		assert maxConnections > 0;

		this.maxConnections = maxConnections;
		manager = SocketManager.newSocketManager(maxConnections);
	}

	public RubusServer() {
		this(8);
	}

	@Override
	public void run() {
		while (isRunning) {
			try {
				if (manager.size() < maxConnections) {
					RubusSocket socket = serverSocket.accept(50);
					manager.add(socket);
				}
			} catch (SocketTimeoutException ignored) {}
		}
	}

	public void terminate() {
		isRunning = false;
		manager.terminate();
		serverSocket.close();
	}

	public boolean isRunning() {
		return isRunning;
	}

	public int getMaximumOpenedConnections() {
		return maxConnections;
	}
}
