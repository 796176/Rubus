/*
 * Rubus is an application layer protocol for video and audio streaming and
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SecureServerSocketDecorator performs a secure handshake for every instance of {@link RubusSocket} returned by
 * the underlying {@link RubusServerSocket}. If both client and server sockets support secure connection, the returned
 * socket is an instance of {@link SecureSocket}; if only one socket supports secure connection and both client and
 * socket allow for unsecure connection, the returned socket is the same instance returned by the underlying
 * RubusServerSocket; otherwise, the connection is not established.<br>
 * In order to perform a secure handshake, the client socket needs to be passed to a {@link SecureSocket} constructor
 * and specify that this SecureSocket is a handshake initiator.<br>
 */
public class SecureServerSocketDecorator implements RubusServerSocket {

	private record SharedSocket(boolean shared, RubusSocket socket) {}

	private final static Logger logger = LoggerFactory.getLogger(SecureServerSocketDecorator.class);

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
	 * Constructs an instance of this class.
	 * @param serverSocket the underlying instance of RubusServerSocket
	 * @param config the config containing the necessary fields to perform secure handshakes
	 * @param openConnectionsLimit limits the number of open sockets
	 * @param handshakeExecutorService the execution service that performs secure handshakes
	 * @param handshakeTimeout the handshake timeout in milliseconds, or 0 to wait indefinitely
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

		if (config.get("secure-connection-required") == null) {
			logger.error("{} not contain secure-connection-required key", config);
			throw new RuntimeException("The secure-connection-required parameter is absent");
		}

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
		logger.debug(
			"""
			{} instantiated, RubusServerSocket: {}, Config: {}, \
			open connections limit: {}, ExecutorService: {}, handshake timeout: {}""",
			this,
			serverSocket,
			config,
			openConnectionsLimit,
			handshakeExecutorService,
			handshakeTimeout
		);
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
		} while (!isClosed() && socket == null);
		if (isClosed()) throw new SocketException();
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
		} while (!isClosed() && timePassed < timeout && socket == null);
		if (timePassed >= timeout) throw new SocketTimeoutException();
		if (isClosed()) throw new SocketException();
		return socket;
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
			logger.info("ExecutionService {} shutdown interrupted in {}", executorService, this, e);
		} finally {
			for (SharedSocket ss: sharedSockets) {
				if (ss != null && !ss.shared()) {
					try {
						ss.socket().close();
					} catch (IOException e) {
						logger.warn("{} could not close {}", this, ss.socket(), e);
					}
				}
			}
		}
		logger.debug("{} closed", this);
	}

	@Override
	public boolean isClosed() {
		return isTerminated;
	}

	private synchronized boolean put(RubusSocket socket) {
		if (isClosed()) return false;
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
		if (isClosed()) return null;
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

		private BackgroundThread() {
			logger.debug("{} instantiated", this);
		}

		@Override
		public void run() {
			while (!isClosed()) {
				try {
					if (openConnections < connectionLimit) {
						RubusSocket socket = serverSocket.accept();
						openConnections++;
						executorService.submit(new HandshakePerformer(socket));
					}
				} catch (IOException e) {
					logger.warn("{} could not establish connection", this, e);
				}
			}
		}
	}

	private class HandshakePerformer implements Runnable {

		private final RubusSocket localSocket;

		private HandshakePerformer(RubusSocket socket) {
			assert socket != null;

			localSocket = socket;
			logger.debug("{} instantiated, RubusSocket: {}", this, socket);
		}

		@Override
		public void run() {
			try {
				RubusSocket secureSocket =
					new SecureSocket(localSocket, config, handshakeTimeout, false);
				logger.debug("{} established secure connection on {}", this, localSocket);
				if (!put(secureSocket)) {
					try {
						secureSocket.close();
					} catch (IOException e) {
						logger.warn("{} failed to close connection on {}", this, secureSocket);
					}
					openConnections--;
				}
			} catch (HandshakeFailedException e) {
				logger.debug("{} failed handshake on {}", this, localSocket, e);
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
				} catch (IOException ioException) {
					logger.warn("{} failed to close connection on {}", this, localSocket, ioException);
				}
			} catch (IOException | InterruptedException e) {
				logger.debug("{} failed handshake on {}", this, localSocket, e);
				try {
					openConnections--;
					this.localSocket.close();
				} catch (IOException ioException) {
					logger.warn("{} failed to close connection on {}", this, localSocket, ioException);
				}
			}
		}
	}
}
