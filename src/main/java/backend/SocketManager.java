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

public class SocketManager extends Thread {

	private final ConcurrentLinkedQueue<RubusSocket> sockets = new ConcurrentLinkedQueue<>();

	private final ExecutorService executorService;

	private boolean isTerminated = false;

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
				executorService.submit(new RequestHandler(socket, this::add));
			} catch (InterruptedException ignored) {}
		}
	}

	public static SocketManager newSocketManager(int poolSize) {
		SocketManager socketManager = new SocketManager(poolSize);
		socketManager.start();
		return socketManager;
	}

	public void add(RubusSocket socket) {
		assert socket != null;

		boolean isEmpty = sockets.isEmpty();
		sockets.add(socket);
		if (isEmpty) {
			synchronized (sockets) { sockets.notify(); }
		}
	}

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

	public int size() {
		return sockets.size();
	}
}
