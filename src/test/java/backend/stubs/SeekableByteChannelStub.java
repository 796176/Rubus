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

package backend.stubs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

public class SeekableByteChannelStub implements SeekableByteChannel {

	public byte[] backingArray;

	public int position;

	public int offset;

	public int size;

	public boolean isOpen = true;

	public SeekableByteChannelStub(byte[] array) {
		backingArray = array;
		position = 0;
		offset = 0;
		size = array.length;
	}

	@Override
	public synchronized int read(ByteBuffer byteBuffer) throws IOException {
		if (byteBuffer == null) throw new NullPointerException();
		if (!isOpen()) throw new ClosedChannelException();
		if (byteBuffer.isReadOnly()) throw new IllegalArgumentException();
		if (byteBuffer.remaining() == 0) return 0;
		if (position() >= size()) return -1;

		int byteRead = (int) Math.min(size() - position(), byteBuffer.remaining());
		byteBuffer.put(backingArray, (int) position() + offset, byteRead);
		position(position() + byteRead);
		return byteRead;
	}

	@Override
	public int write(ByteBuffer byteBuffer) {
		throw  new NonWritableChannelException();
	}

	@Override
	public long position() throws IOException {
		if (!isOpen()) throw new ClosedChannelException();
		return position;
	}

	@Override
	public synchronized SeekableByteChannel position(long l) throws IOException {
		if (!isOpen()) throw new ClosedChannelException();
		int newPosition = (int) l;
		if (newPosition < 0) throw new IllegalArgumentException();
		position = newPosition;
		return this;
	}

	@Override
	public long size() throws IOException{
		if (!isOpen()) throw new ClosedChannelException();
		return size;
	}

	@Override
	public SeekableByteChannel truncate(long l) {
		return null;
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public synchronized void close() {
		isOpen = false;
	}
}
