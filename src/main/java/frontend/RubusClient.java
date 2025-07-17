/*
 * Rubus is an application layer protocol for video and audio streaming and
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * RubusClient is an auxiliary class designed to simplify sending rubus request messages and receiving rubus response
 * messages.
 */
public class RubusClient implements AutoCloseable {

	private final Logger logger = LoggerFactory.getLogger(RubusClient.class);

	private RubusSocket socket;

	private Supplier<RubusSocket> socketSupplier;

	/**
	 * Constructs an instance of this class.
	 * @param socketSupplier the socket supplier that instantiates sockets connected to the server
	 */
	public RubusClient(Supplier<RubusSocket> socketSupplier) {
		assert socketSupplier != null;

		setSocketSupplier(socketSupplier);
		socket = socketSupplier.get();

		logger.debug("{} instantiated, Supplier: {}", this, socketSupplier);
	}

	/**
	 * Sets a new socket supplier. If it's necessary to reinstantiate the already created sockets using this socket
	 * supplier, the invocation of {@link #close()} is required.
	 * @param socketSupplier the socket supplier that instantiates sockets connected to the server
	 */
	public void setSocketSupplier(Supplier<RubusSocket> socketSupplier) {
		assert socketSupplier != null;

		this.socketSupplier = socketSupplier;
	}

	/**
	 * Returns the current socket supplier.
	 * @return the current socket supplier
	 */
	public Supplier<RubusSocket> getSocketSupplier() {
		return socketSupplier;
	}

	/**
	 * Sends the request message and blocks until the response message is received. 0 timeout makes the method to wait
	 * indefinitely. If the request-response message exchange is not completed within the specified time the exception
	 * is thrown.
	 * @param request the request message
	 * @param timeout the timeout in milliseconds
	 * @return the response message
	 * @throws IOException if some I/O error occur
	 * @throws SocketTimeoutException if the request-response message exchange is not complete within the timeout
	 */
	public RubusResponse send(RubusRequest request, long timeout) throws InterruptedException, IOException {
		assert request != null && timeout >= 0;

		try {
			long sendingStartsTime = System.currentTimeMillis();
			if (socket.isClosed()) socket = socketSupplier.get();

			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<?> future = executor.submit(() -> {
				socket.write(request.getBytes());
				return null;
			});
			executor.shutdown();
			if(!executor.awaitTermination(timeout > 0 ? timeout : Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
				throw new SocketTimeoutException();
			} else if (future.state() == Future.State.FAILED) {
				if (future.exceptionNow() instanceof IOException ioException) {
					throw ioException;
				} else if (future.exceptionNow() instanceof RuntimeException runtimeException) {
					throw runtimeException;
				}
			}

			long timeSpentToSend = System.currentTimeMillis() - sendingStartsTime;
			if (timeout != 0 && timeout - timeSpentToSend <= 0) throw new SocketTimeoutException();
			byte[] response = RubusSockets.extractMessage(socket, timeout > 0 ? timeout - timeSpentToSend : 0);
			return new RubusResponse(response);
		} catch (IOException | InterruptedException e) {
			socket.close();
			throw e;
		}
	}

	@Override
	public void close() throws IOException {
		socket.close();

		logger.debug("{} closed", this);
	}
}

