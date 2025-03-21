/*
 * Rubus is an application level protocol for video and audio streaming and
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

package auxiliary;

import common.RubusSocket;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class DummySocket implements RubusSocket {

	public int occupied = 0;

	public final byte[] buffer;

	public long sTime = System.currentTimeMillis();

	public long rTime = System.currentTimeMillis();

	public long oTime = System.currentTimeMillis();

	public long cTime;

	public boolean isClosed = false;

	public boolean blockReading = false;

	public DummySocket(int bufferSize) {
		assert bufferSize > 0;

		buffer = new byte[bufferSize];
	}

	@Override
	public void close() {
		isClosed = true;
		cTime = System.currentTimeMillis();
	}

	@Override
	public void close(long timeout) {
		close();
	}

	@Override
	public int read(byte[] in) {
		if (isClosed || blockReading) return -1;
		int deOccupied = Math.min(in.length, occupied);
		System.arraycopy(buffer, 0, in, 0, deOccupied);
		occupied -= deOccupied;
		System.arraycopy(buffer, deOccupied, buffer, 0, occupied);
		return deOccupied;
	}

	@Override
	public int read(byte[] in, long timeout) throws IOException {
		if (isClosed || blockReading) return -1;
		if (in.length == 0) return 0;
		if (occupied == 0) throw new SocketTimeoutException("No data, pal");
		return read(in);
	}

	@Override
	public int read(byte[] in, int offset, int length) {
		if (isClosed || blockReading) return -1;
		int deOccupied = Math.min(occupied, length);
		System.arraycopy(buffer, 0, in, offset, deOccupied);
		occupied -= deOccupied;
		System.arraycopy(buffer, deOccupied, buffer, 0, occupied);
		return deOccupied;
	}

	@Override
	public int read(byte[] in, int offset, int length, long timeout) throws IOException {
		if (isClosed || blockReading) return -1;
		if (length == 0) return 0;
		if (occupied == 0) throw new SocketTimeoutException("No data, pal");
		return read(in, offset, length);
	}

	@Override
	public void write(byte[] out) throws IOException {
		if (buffer.length - occupied < out.length) throw new IOException("Low on memory");
		System.arraycopy(out, 0, buffer, occupied, out.length);
		occupied += out.length;
	}

	@Override
	public void write(byte[] out, int offset, int length) throws IOException {
		if (buffer.length - occupied < length) throw new IOException("Low on memory");
		System.arraycopy(out, offset, buffer, occupied, length);
		occupied += length;
	}

	@Override
	public boolean isClosed() {
		return false;
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
