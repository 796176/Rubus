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

package backend;

import common.RubusSocket;

import java.io.IOException;

/**
 * RubusServerSocket interface provides a few factory methods to instantiate {@link RubusSocket}. The instantiated
 * socket is bound to the client socket, which initiated the connection.
 */
public interface RubusServerSocket {

	/**
	 * Blocks until the connection is established.
	 * @return a new socket
	 * @throws IOException if some I/O error occurs
	 */
	RubusSocket accept() throws IOException;

	/**
	 * Blocks until the connection is established. If the connection is not established within the timeout,
	 * the exception is thrown.
	 * @param timeout the timeout in milliseconds
	 * @return a new socket
	 * @throws IOException if some I/O error occurs
	 */
	RubusSocket accept(long timeout) throws IOException;

	/**
	 * Releases the associated resources.
	 * @throws IOException if some I/O error occurs
	 */
	void close() throws IOException;

	/**
	 * Returns true if this RubusServerSocket has been closed, false otherwise.
	 * @return true if this RubusServerSocket has been closed, false otherwise
	 */
	boolean isClosed();
}
