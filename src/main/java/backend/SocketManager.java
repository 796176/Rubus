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

import backend.io.MediaPool;
import common.RubusSocket;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * SocketManager keeps track of open connections. It's responsible for handing the incoming requests, closing
 * the connections or keeping them open.
 */
public class SocketManager extends Thread {

	private final ConcurrentLinkedQueue<RubusSocket> sockets = new ConcurrentLinkedQueue<>();

	private final ExecutorService executorService;

	private boolean isTerminated = false;

	private int activeConnections = 0;

	private final MediaPool mediaPool;

	private final RequestParserStrategy requestParserStrategy;

	private SocketManager(MediaPool mediaPool, ExecutorService requestExecutorService, RequestParserStrategy requestParserStrategy) {
		assert mediaPool != null && requestExecutorService != null && requestParserStrategy != null;

		this.mediaPool = mediaPool;
		executorService = requestExecutorService;
		this.requestParserStrategy = requestParserStrategy;
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
				executorService.submit(
					new RequestHandler(
						mediaPool, socket, requestParserStrategy.clone(), this::keepConnection, this::closeConnection
					)
				);
			} catch (InterruptedException ignored) {}
		}
	}

	/**
	 * Constructs a new SocketManager and immediately starts handling the incoming requests.
	 * @param mediaPool the media pool containing the available media
	 * @param requestExecutorService the executor service that performs request handling
	 * @param requestParserStrategy the parser strategy to use
	 * @return a new instance of SocketManager
	 */
	public static SocketManager newSocketManager(
		MediaPool mediaPool,
		ExecutorService requestExecutorService,
		RequestParserStrategy requestParserStrategy
	) {
		SocketManager socketManager = new SocketManager(mediaPool, requestExecutorService, requestParserStrategy);
		socketManager.start();
		return socketManager;
	}

	/**
	 * Adds a socket to handle its requests.
	 * @param socket the socket requests of which need to be handled
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
	 * Terminate this SocketManager and closes all the open connections.
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
	 * Returns the number of the currently open connections.
	 * @return the number of the currently open connections
	 */
	public int getOpenConnections() {
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
