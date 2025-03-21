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

package common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * TCPRubusSocket is a concrete implementation of the {@link RubusSockets} interface. It uses TCP as its transport
 * protocol, so it guarantees that the sent packets arrive and do so in the same order they were sent.
 */
public class TCPRubusSocket implements RubusSocket{

	private final long openedTime;

	private long closedTime;

	private long lastReceived;

	private long lastSent;

	private final Socket underlyingSocket;
	private final int defaultTimeout;

	/**
	 * Construct an instance of this class.
	 * @param inetAddress a server address
	 * @param port a server port
	 * @throws IOException if some I/O error occurs
	 */
	public TCPRubusSocket(InetAddress inetAddress, int port) throws IOException {
		assert inetAddress != null;

		underlyingSocket = new Socket(inetAddress, port);
		openedTime = System.currentTimeMillis();
		lastSent = openedTime;
		lastReceived = openedTime;
		defaultTimeout = underlyingSocket.getSoTimeout();
	}

	/**
	 * Constructs an instance of this class.
	 * @param socket an opened socket
	 * @param openedTime the time the connection was established
	 * @throws IOException if some I/O error occurs
	 */
	public TCPRubusSocket(Socket socket, long openedTime) throws IOException {
		assert socket != null;

		underlyingSocket = socket;
		this.openedTime = openedTime;
		lastSent = openedTime;
		lastReceived = openedTime;
		defaultTimeout = socket.getSoTimeout();
	}

	@Override
	public void close() throws IOException {
		underlyingSocket.close();
		closedTime = System.currentTimeMillis();
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
		lastReceived = System.currentTimeMillis();
		return byteRead;
	}

	@Override
	public int read(byte[] in, long timeout) throws IOException {
		underlyingSocket.setSoTimeout((int) timeout);
		int	byteRead = read(in);
		underlyingSocket.setSoTimeout(defaultTimeout);
		return byteRead;
	}

	@Override
	public int read(byte[] in, int offset, int length) throws IOException {
		int byteRead = underlyingSocket.getInputStream().read(in, offset, length);
		lastReceived = System.currentTimeMillis();
		return byteRead;
	}

	@Override
	public int read(byte[] in, int offset, int length, long timeout) throws IOException {
		underlyingSocket.setSoTimeout((int) timeout);
		int byteRead = underlyingSocket.getInputStream().read(in, offset, length);
		underlyingSocket.setSoTimeout(defaultTimeout);
		return byteRead;
	}

	@Override
	public void write(byte[] out) throws IOException {
		underlyingSocket.getOutputStream().write(out);
		lastSent = System.currentTimeMillis();
	}

	@Override
	public void write(byte[] out, int offset, int length) throws IOException {
		underlyingSocket.getOutputStream().write(out, offset, length);
		lastSent = System.currentTimeMillis();
	}

	@Override
	public boolean isClosed() {
		return underlyingSocket.isClosed();
	}

	@Override
	public long openTime() {
		return openedTime;
	}

	@Override
	public long closeTime() {
		return closedTime;
	}

	@Override
	public long lastReceiveTime() {
		return lastReceived;
	}

	@Override
	public long lastSendTime() {
		return lastSent;
	}
}
