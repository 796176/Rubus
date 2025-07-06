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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * TCPRubusSocket is a concrete implementation of the {@link RubusSockets} interface. It uses TCP as its transport
 * protocol, so it guarantees that the sent packets arrive and do so in the same order they were sent.
 */
public class TCPRubusSocket implements RubusSocket{

	private final Logger logger = LoggerFactory.getLogger(TCPRubusSocket.class);

	private final long oTime;

	private long cTime = 0;

	private long rTime = 0;

	private long sTime = 0;

	private final Socket underlyingSocket;
	private final int defaultTimeout;

	/**
	 * Construct an instance of this class.
	 * @param inetAddress a server address
	 * @param port a server port
	 * @throws IOException if some I/O error occurs
	 */
	public TCPRubusSocket(InetAddress inetAddress, int port) throws IOException {
		this(new Socket(inetAddress, port), System.currentTimeMillis());
	}

	/**
	 * Constructs an instance of this class.
	 * @param socket an opened socket
	 * @param openTime the time the connection was established
	 * @throws IOException if some I/O error occurs
	 */
	public TCPRubusSocket(Socket socket, long openTime) throws IOException {
		assert socket != null;

		underlyingSocket = socket;
		this.oTime = openTime;
		defaultTimeout = socket.getSoTimeout();

		logger.debug("{} instantiated, Socket: {}, open time: {}", this, socket, openTime);
	}

	@Override
	public void close() throws IOException {
		underlyingSocket.close();
		cTime = System.currentTimeMillis();

		logger.debug("{} closed", this);
	}

	@Override
	public void close(long timeout) throws IOException {
		underlyingSocket.setSoTimeout((int) timeout);
		close();
		underlyingSocket.setSoTimeout(defaultTimeout);
	}

	@Override
	public int read(byte[] in) throws IOException {
		int byteRead = underlyingSocket.getInputStream().read(in);
		rTime = System.currentTimeMillis();

		logger.debug("{} read {} bytes into buffer of {} size", this, byteRead, in.length);

		return byteRead;
	}

	@Override
	public int read(byte[] in, long timeout) throws IOException {
		underlyingSocket.setSoTimeout((int) timeout);
		int	byteRead = read(in);
		underlyingSocket.setSoTimeout(defaultTimeout);

		logger.debug("{} read {} bytes into buffer of {} size", this, byteRead, in.length);

		return byteRead;
	}

	@Override
	public int read(byte[] in, int offset, int length) throws IOException {
		int byteRead = underlyingSocket.getInputStream().read(in, offset, length);
		rTime = System.currentTimeMillis();

		logger.debug(
			"{} read {} bytes into buffer of {} size with {} buffer offset and {} available length ",
			this,
			byteRead,
			in.length,
			offset,
			length
		);

		return byteRead;
	}

	@Override
	public int read(byte[] in, int offset, int length, long timeout) throws IOException {
		underlyingSocket.setSoTimeout((int) timeout);
		int byteRead = underlyingSocket.getInputStream().read(in, offset, length);
		underlyingSocket.setSoTimeout(defaultTimeout);

		logger.debug(
			"{} read {} bytes into buffer of size {} with buffer offset {} and available length {}",
			this,
			byteRead,
			in.length,
			offset,
			length
		);

		return byteRead;
	}

	@Override
	public void write(byte[] out) throws IOException {
		underlyingSocket.getOutputStream().write(out);
		sTime = System.currentTimeMillis();

		logger.debug("{} wrote buffer of {} size", this, out.length);
	}

	@Override
	public void write(byte[] out, int offset, int length) throws IOException {
		underlyingSocket.getOutputStream().write(out, offset, length);
		sTime = System.currentTimeMillis();

		logger.debug(
			"{} wrote buffer of {} size with {} buffer offset and {} length",
			this,
			out.length,
			offset,
			length
		);
	}

	@Override
	public boolean isClosed() {
		return underlyingSocket.isClosed();
	}

	@Override
	public long openTime() {
		return oTime;
	}

	@Override
	public long closeTime() {
		return cTime;
	}

	@Override
	public long lastReceiveTime() {
		return rTime;
	}

	@Override
	public long lastSendTime() {
		return sTime;
	}
}
