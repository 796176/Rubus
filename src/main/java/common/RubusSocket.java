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

package common;


import java.io.IOException;

/**
 * RubusSocket is an interface designed for the client and the server to communicate with each other using its methods.
 * The client initiates the connection using a server address and a server port. The server, in turn, accepts or refuses
 * connection. If it's the former the connection is established, and the client and the server can send data to each
 * other.<br><br>
 * Its concrete implementations may guaranty data integrity, p2p encryption, data security, end-point authentication,
 * and other MITM attack protection, or provide connection-less communication.<br><br>
 * RubusSocket can be created indirectly via {@link RubusSockets}; for that reason the concrete implementations must
 * have one public constructor with {@link java.net.InetAddress} as a server address and int as a server port parameters.
 */
public interface RubusSocket {

	/**
	 * Closes the established connection.
	 * @throws IOException if some I/O error occurs
	 */
	void close() throws IOException;

	/**
	 * Closes the established connection.
	 * @param timeout the timeout
	 * @throws IOException if some I/O error occurs
	 */
	void close(long timeout) throws IOException;

	/**
	 * Reads received bytes into the array.<br>
	 * Not necessarily reads all available bytes during its invocation.
	 * @param in the array data is written into
	 * @return the number of bytes read
	 * @throws IOException if some I/O error occurs
	 */
	int read(byte[] in) throws IOException;

	/**
	 * Reads received bytes into the array. If no bytes are available it waits the specified time. If no byte received
	 * after the timeout it throws an exception.<br>
  	 * Not necessarily reads all available bytes during its invocation.
	 * @param in the array data is written into
	 * @param timeout the timeout in milliseconds
	 * @return the numbers of bytes read
	 * @throws IOException if some I/O error occurs
	 */
	int read(byte[] in, long timeout) throws IOException;

	/**
	 * Reads received bytes into the array.<br>
	 * Not necessarily reads all available bytes during its invocation.
	 * @param in the array data is written into
	 * @param offset the index of the array the first byte is written into
	 * @param length the length of the available array
	 * @return the number of byte read
	 * @throws IOException if some I/O error occurs
	 */
	int read(byte[] in, int offset, int length) throws IOException;

	/**
	 * Reads received bytes into the array. If no bytes are available it waits the specified time. If no byte received
	 * after the timeout it throws an exception.<br>
	 * Not necessarily reads all available bytes during its invocation.
	 * @param in the array data is written into
	 * @param offset the index of the array the first byte is written into
	 * @param length the length of the available array
	 * @param timeout the timeout in milliseconds
	 * @return the number of byte read
	 * @throws IOException if some I/O error occurs
	 */
	int read(byte[] in, int offset, int length, long timeout) throws IOException;

	/**
	 * Sends bytes to the receiving side.
	 * @param out data to send
	 * @throws IOException if some I/O error occurs
	 */
	void write(byte[] out) throws IOException;

	/**
	 * Sends byte to the receiving side.
	 * @param out the data to send
	 * @param offset the index of the first byte to send
	 * @param length the number of bytes to send
	 * @throws IOException if some I/O error occurs
	 */
	void write(byte[] out, int offset, int length) throws IOException;

	/**
	 * Returns true if the established connection is closed, false otherwise.
	 * @return true if the established connection is closed, false otherwise
	 */
	boolean isClosed();

	/**
	 * Returns the time in milliseconds when the connection was established.
	 * @return the time in milliseconds when the connection was established
	 */
	long openTime();

	/**
	 * Returns the time in milliseconds when the connection was closed.
	 * @return the time in milliseconds when the connection was closed
	 */
	long closeTime();

	/**
	 * Returns the time in milliseconds of recently received data, or 0 if no data has been received.
	 * @return the time in milliseconds of recently received data, or 0 if no data has been received
	 */
	long lastReceiveTime();

	/**
	 * Returns the time in milliseconds of recently sent data, or 0 if no data has been sent.
	 * @return the time in milliseconds of recently sent data, or 0 if no data has been sent
	 */
	long lastSendTime();
}
