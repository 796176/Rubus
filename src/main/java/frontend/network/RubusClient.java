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

package frontend.network;

import jakarta.annotation.Nonnull;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * RubusClient is an interface that streamlines sending rubus request messages and receiving rubus response messages.
 */
public interface RubusClient extends Closeable {

	/**
	 * Sends the request message and blocks until the response message is received. 0 timeout makes the method to wait
	 * indefinitely. If the request-response message exchange is not completed within the specified time, the exception
	 * is thrown.
	 * @param request the request message
	 * @param timeout the timeout in milliseconds
	 * @return the response message
	 * @throws IOException if some I/O error occur
	 * @throws SocketTimeoutException if the request-response message exchange is not complete within the timeout
	 * @throws InterruptedException if the operation is interrupted
	 */
	RubusResponse send(@Nonnull RubusRequest request, long timeout) throws InterruptedException, IOException;

	/**
	 * Returns the builder to use to instantiate {@link RubusRequest}s associated with this RubusClient.
	 * @return the {@link RubusRequest.Builder} instance
	 */
	RubusRequest.Builder getRequestBuilder();
}

