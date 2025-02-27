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
import java.util.concurrent.*;

/**
 * SocketManager keeps track of opened connections. It responsible for order in which connections are treated, closing
 * them or keeping them opened.
 */
public class SocketManager extends Thread {

	private final ConcurrentLinkedQueue<RubusSocket> sockets = new ConcurrentLinkedQueue<>();

	private final ExecutorService executorService;

	private boolean isTerminated = false;

	private int activeConnections = 0;

	private SocketManager(int poolSize) {
		assert poolSize > 0;

		executorService = Executors.newFixedThreadPool(poolSize);
	}

	@Override
	public void run() {
		while (!isTerminated) {
			try {
				RubusSocket socket;
				while ((socket = sockets.poll()) == null) {
					synchronized (sockets) {
						if (isTerminated) return;
						sockets.wait();
					}
				}
				executorService.submit(new RequestHandler(socket, this::keepConnection, this::closeConnection));
			} catch (InterruptedException ignored) {}
		}
	}

	/**
	 * Constructs this class using the fixed amount of threads and starts the connection management process in
	 * a different thread.
	 * @param poolSize the number of how many connections can be treated simultaneously
	 * @return an constructed instance
	 */
	public static SocketManager newSocketManager(int poolSize) {
		SocketManager socketManager = new SocketManager(poolSize);
		socketManager.start();
		return socketManager;
	}

	/**
	 * Adds an opened connection to further proceeds its requests.
	 * @param socket an opened connection
	 */
	public void add(RubusSocket socket) {
		assert socket != null;

		activeConnections++;
		sockets.add(socket);
		if (SocketManager.this.getState() == State.WAITING) {
			synchronized (sockets) { sockets.notify(); }
		}
	}

	/**
	 * Terminate the connection management process and closes all the connections.
	 */
	public void terminate() {
		isTerminated = true;
		if (sockets.isEmpty()) synchronized (sockets) { sockets.notify(); }
		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			for (RubusSocket socket: sockets) {
				socket.close(1000);
			}
		} catch (InterruptedException | IOException ignored) {}
	}

	/**
	 * Returns the number of currently opened connections.
	 * @return the number of currently opened connections
	 */
	public int getActiveConnections() {
		return activeConnections;
	}

	private void keepConnection(RubusSocket socket) {
		sockets.add(socket);
		if (SocketManager.this.getState() == State.WAITING) {
			synchronized (sockets) { sockets.notify(); }
		}
	}

	private void closeConnection(RubusSocket socket) {
		try {
		socket.close();
		} catch (IOException ignored) {}
		activeConnections--;
	}
}
