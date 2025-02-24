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

public class TCPRubusSocket implements RubusSocket{

	long openedTime;
	long closedTime;
	long lastReceived;
	long lastSent;

	private final Socket underlyingSocket;
	private final int defaultTimeout;
	public TCPRubusSocket(InetAddress inetAddress, int port) throws IOException {
		assert inetAddress != null;

		underlyingSocket = new Socket(inetAddress, port);
		openedTime = System.currentTimeMillis();
		defaultTimeout = underlyingSocket.getSoTimeout();
	}

	public TCPRubusSocket(Socket socket, long openedTime) throws IOException {
		assert socket != null;

		underlyingSocket = socket;
		this.openedTime = openedTime;
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
	public void write(byte[] out, long timeout) throws IOException {
		underlyingSocket.setSoTimeout((int) timeout);
		underlyingSocket.getOutputStream().write(out);
		underlyingSocket.setSoTimeout(defaultTimeout);
	}

	@Override
	public void write(byte[] out, int offset, int length) throws IOException {
		underlyingSocket.getOutputStream().write(out, offset, length);
		lastSent = System.currentTimeMillis();
	}

	@Override
	public void write(byte[] out, int offset, int length, long timeout) throws IOException {
		underlyingSocket.setSoTimeout((int) timeout);
		underlyingSocket.getOutputStream().write(out, offset, length);
		underlyingSocket.setSoTimeout(defaultTimeout);
	}

	@Override
	public boolean isClosed() {
		return underlyingSocket.isClosed();
	}

	@Override
	public long openedTime() {
		return 0;
	}

	@Override
	public long closedTime() {
		return 0;
	}

	@Override
	public long lastReceivedTime() {
		return 0;
	}

	@Override
	public long lastSentTime() {
		return 0;
	}
}
