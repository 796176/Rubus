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

import common.Config;
import common.RubusSocket;
import common.ssl.HandshakeFailedException;
import common.ssl.SecureSocket;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SecureServerSocketDecorator is designed to augment an instance of {@link RubusServerSocket} by establishing secure
 * connections on sockets returned by the accept methods. The reasoning behind creating this class was (1) to isolate
 * {@link RubusServerSocket} and the client that the sockets use a secure connection and (2) to allow establishing many
 * ssl handshakes at a time. The implementation of the latter is achieved by continuous establishing regular
 * connections and performing handshakes in separate threads.<br<br>
 *
 * The client may have a limit on the amount of established connections, and this class won't go over this limit if
 * their limits are the same. It's achieved by SecureServerSocketDecorator storing references to every socket even ones
 * it already passed to the client. So if the client closes one of its sockets, SecureServerSocketDecorator sees it and
 * allowing itself to establish a new connection if before that the limit had been reached.
 */
public class SecureServerSocketDecorator implements RubusServerSocket {

	private record SharedSocket(boolean shared, RubusSocket socket) {}

	private final RubusServerSocket serverSocket;

	private final Config config;

	private final int connectionLimit;

	private final boolean scRequired;

	private final long handshakeTimeout;

	private final SharedSocket[] sharedSockets;

	private final BackgroundThread backgroundThread = new BackgroundThread();

	private int openConnections = 0;

	private boolean isTerminated = false;

	private ExecutorService executorService;

	/**
	 * Constructs an instance of this class and immediately starts accepting new sockets and establishing secure
	 * connections. openConnectionsLimit sets a limit of how many secure connections SecureServerSocketDecorator can
	 * keep open at a time; setting it to zero means secure connections will be established one at a time in the same
	 * thread and SecureServerSocketDecorator won't store references to sockets as well; this significantly worsens
	 * the performance so it's intended for debugging only. handshakeExecutorService is responsible for performing
	 * handshakes. handshakeTimeout is a timeout of the handshake. The config instance must contain
	 * the "secure-connection-required" value to determine if unsecure connections are allowed.
	 * @param serverSocket the instance of RubusServerSocket that establishes actual connections
	 * @param config the config containing the necessary values
	 * @param openConnectionsLimit the limit of how many connections this SecureServerSocketDecorator can keep open
	 * @param handshakeExecutorService the execution service that performs handshakes
	 * @param handshakeTimeout the handshake timeout in milliseconds
	 */
	public SecureServerSocketDecorator(
		RubusServerSocket serverSocket,
		Config config,
		int openConnectionsLimit,
		ExecutorService handshakeExecutorService,
		long handshakeTimeout
	) {
		assert serverSocket != null &&
			   config != null &&
			   openConnectionsLimit >= 0 &&
			   handshakeExecutorService != null &&
			   handshakeTimeout >= 0;

		this.serverSocket = serverSocket;
		this.config = config;
		connectionLimit = openConnectionsLimit;
		scRequired = Boolean.parseBoolean(config.get("secure-connection-required"));
		sharedSockets = new SharedSocket[connectionLimit];
		this.handshakeTimeout = handshakeTimeout;
		if (openConnectionsLimit > 0) {
			executorService = handshakeExecutorService;
			backgroundThread.start();
		}
	}

	@Override
	public RubusSocket accept() throws IOException {
		if (connectionLimit == 0) {
			try {
				return new SecureSocket(serverSocket.accept(), config, handshakeTimeout, false);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}

		RubusSocket socket;
		do {
			socket = get();
		} while (!isTerminated && socket == null);
		if (isTerminated) throw new SocketTimeoutException();
		else return socket;
	}

	@Override
	public RubusSocket accept(long timeout) throws IOException {
		if (connectionLimit == 0) {
			try {
				return new SecureSocket(serverSocket.accept(timeout), config, handshakeTimeout, false);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}

		long acceptStartsTime = System.currentTimeMillis();
		long timePassed;
		RubusSocket socket;
		do {
			socket = get();
			timePassed = System.currentTimeMillis() - acceptStartsTime;
		} while (!isTerminated && timePassed < timeout && socket == null);
		if (isTerminated || timePassed >= timeout) throw new SocketTimeoutException();
		else return socket;
	}

	@Override
	public void close() throws IOException {
		try {
			isTerminated = true;
			serverSocket.close();
			backgroundThread.join();
			executorService.shutdown();
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new IOException();
		} finally {
			for (SharedSocket ss: sharedSockets) {
				if (ss != null && !ss.shared()) {
					try {
						ss.socket().close();
					} catch (IOException e) {

					}
				}
			}
		}
	}

	private synchronized boolean put(RubusSocket socket) {
		if (isTerminated) return false;
		for (int i = 0; i < sharedSockets.length; i++) {
			SharedSocket ss = sharedSockets[i];
			if (ss == null || ss.shared() && ss.socket().isClosed()) {
				sharedSockets[i] = new SharedSocket(false, socket);
				return true;
			}
		}
		return false;
	}

	private synchronized RubusSocket get() {
		if (isTerminated) return null;
		RubusSocket retVal = null;
		for (int i = 0; i < sharedSockets.length; i++) {
			SharedSocket ss = sharedSockets[i];
			if (ss == null) continue;
			if (retVal == null && !ss.shared()) {
				sharedSockets[i] = new SharedSocket(true, ss.socket);
				retVal = ss.socket();
			} else if (ss.shared() && ss.socket().isClosed()) {
				sharedSockets[i] = null;
				openConnections--;
			};
		}
		return retVal;
	}

	private class BackgroundThread extends Thread {

		@Override
		public void run() {
			while (!isTerminated) {
				try {
					if (openConnections < connectionLimit) {
						RubusSocket socket = serverSocket.accept();
						openConnections++;
						executorService.submit(new HandshakePerformer(socket));
					}
				} catch (IOException e) {

				}
			}
		}
	}

	private class HandshakePerformer implements Runnable {

		private final RubusSocket localSocket;

		private HandshakePerformer(RubusSocket socket) {
			assert socket != null;

			localSocket = socket;
		}

		@Override
		public void run() {
			try {
				RubusSocket secureSocket =
					new SecureSocket(localSocket, config, handshakeTimeout, false);
				if (!put(secureSocket)) {
					secureSocket.close();
					openConnections--;
				}
			} catch (HandshakeFailedException e) {
				try {
					if (!scRequired) {
						if(!put(localSocket)) {
							openConnections--;
							localSocket.close();
						}
					} else {
						openConnections--;
						localSocket.close();
					}
				} catch (IOException ignored) { }
			} catch (IOException | InterruptedException e) {
				try {
					openConnections--;
					this.localSocket.close();
				} catch (IOException ignored) { }
			}
		}
	}
}
