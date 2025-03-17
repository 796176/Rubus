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

package frontend;

import common.RubusSocket;
import common.RubusSockets;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * RubusClient is an auxiliary class designed to simplify sending rubus requests and receiving rubus responses.
 */
public class RubusClient implements AutoCloseable {

	private RubusSocket socket;

	private Supplier<RubusSocket> socketSupplier;

	/**
	 * Constructs this class using a network socket.
	 * @param socketSupplier a socket supplier
	 */
	public RubusClient(Supplier<RubusSocket> socketSupplier) {
		assert socketSupplier != null;

		setSocketSupplier(socketSupplier);
		socket = socketSupplier.get();
	}

	/**
	 * Sets a new socket. If it's necessary to reinitialize the already created sockets using this socket
	 * supplier, the invocation of {@link #close()} is required.
	 * @param socketSupplier a new socket supplier
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
	 * Sends the request and waits to receive the server response. If the sending or the receiving is not done withing
	 * the specified time exits by throwing an exception.
	 * @param request a request to send
	 * @param timeout a timeout
	 * @return a server response
	 * @throws IOException if some I/O error occur
	 */
	public RubusResponse send(RubusRequest request, long timeout) throws InterruptedException, IOException {
		assert request != null && timeout > 0;

		try {
			long sendingStartsTime = System.currentTimeMillis();
			if (socket.isClosed()) socket = socketSupplier.get();

			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<?> future = executor.submit(() -> {
				socket.write(request.getBytes());
				return null;
			});
			executor.shutdown();
			if(!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
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
			byte[] response = RubusSockets.extractMessage(socket, timeout - timeSpentToSend);
			return new RubusResponse(response);
		} catch (IOException | InterruptedException e) {
			socket.close();
			throw e;
		}
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}
}

