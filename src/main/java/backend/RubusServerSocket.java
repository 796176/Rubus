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

/**
 * RubusServerSocket is an interface to establish a connection between a server and a client so that the server can
 * use a {@link RubusSocket} instance to communicate with the client.<br><br>
 * The instance can be indirectly created via {@link common.RubusSockets}; for that reason all concrete implementations
 * must have a public constructor with int as a listening port parameter.
 */
public interface RubusServerSocket {

	/**
	 * Blocks until the connection is established.
	 * @return a new socket
	 * @throws IOException if some I/O error occurs
	 */
	RubusSocket accept() throws IOException;

	/**
	 * Waits specified time and if the connection isn't established throws an exception.
	 * @param timeout the timeout in milliseconds
	 * @return a new socket
	 * @throws IOException is some I/O error occurs
	 */
	RubusSocket accept(long timeout) throws IOException;

	/**
	 * Releases the associated resources.
	 */
	void close();
}
