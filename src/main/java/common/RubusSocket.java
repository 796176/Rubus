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
 * RubusSocket provides interface for the client and the server to exchange messages over the internet.
 */
public interface RubusSocket {

	/**
	 * Closes this socket.
	 * @throws IOException if some I/O error occurs
	 */
	void close() throws IOException;

	/**
	 * Closes this socket. If the socket isn't closed within the specified timeout the exception is thrown.
	 * @param timeout the timeout in milliseconds
	 * @throws IOException if some I/O error occurs
	 */
	void close(long timeout) throws IOException;

	/**
	 * Receives the peer socket's data and writes it into the array.<br>
	 * @param in the array the data is written into
	 * @return the number of bytes read
	 * @throws IOException if some I/O error occurs
	 */
	int read(byte[] in) throws IOException;

	/**
	 * Receives the peer socket's data and writes it into the array. If no byte received within the specified timeout,
	 * the exception is thrown.
	 * @param in the array the data is written into
	 * @param timeout the timeout in milliseconds
	 * @return the number of bytes read
	 * @throws IOException if some I/O error occurs
	 */
	int read(byte[] in, long timeout) throws IOException;

	/**
	 * Receives the peer socket's data and writes it into the array of the specified range.<br>
	 * @param in the array the data is written into
	 * @param offset specifies how many bytes to skip before starting writing into the array
	 * @param length specifies how many bytes are available for writing into the array after offset
	 * @return the number of bytes read
	 * @throws IOException if some I/O error occurs
	 */
	int read(byte[] in, int offset, int length) throws IOException;

	/**
	 * Receives the peer socket's data and writes it into the array of the specified range. If no byte received within
	 * the specified timeout, the exception is thrown.<br>
	 * @param in the array the data is written into
	 * @param offset specifies how many bytes to skip before starting writing into the array
	 * @param length specifies how many bytes are available for writing into the array after offset
	 * @param timeout the timeout in milliseconds
	 * @return the number of bytes read
	 * @throws IOException if some I/O error occurs
	 */
	int read(byte[] in, int offset, int length, long timeout) throws IOException;

	/**
	 * Sends the data read from the array.
	 * @param out the array the data is read from
	 * @throws IOException if some I/O error occurs
	 */
	void write(byte[] out) throws IOException;

	/**
	 * Sends the data read from the array of the specified range.
	 * @param out the array data is read from
	 * @param offset specifies how many bytes to skip before starting reading from the array
	 * @param length specifies how many bytes are available for reading from the array after offset
	 * @throws IOException if some I/O error occurs
	 */
	void write(byte[] out, int offset, int length) throws IOException;

	/**
	 * Returns true if this RubusSocket has been closed, false otherwise.
	 * @return true if this RubusSocket has been closed, false otherwise
	 */
	boolean isClosed();

	/**
	 * Returns the time in milliseconds when this RubusSocket was open.
	 * @return the time in milliseconds when this RubusSocket was open
	 */
	long openTime();

	/**
	 * Returns the time in milliseconds when this RubusSocket was closed.
	 * @return the time in milliseconds when this RubusSocket was closed
	 */
	long closeTime();

	/**
	 * Returns the time in milliseconds of recently received data, or 0 if no data has been received yet.
	 * @return the time in milliseconds of recently received data, or 0 if no data has been received yet
	 */
	long lastReceiveTime();

	/**
	 * Returns the time in milliseconds of recently sent data, or 0 if no data has been sent yet.
	 * @return the time in milliseconds of recently sent data, or 0 if no data has been sent yet
	 */
	long lastSendTime();
}
