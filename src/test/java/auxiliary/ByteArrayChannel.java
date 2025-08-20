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

package auxiliary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

public class ByteArrayChannel implements SeekableByteChannel {

	public byte[] array;

	public int offset;

	public int size;

	public int position;

	public boolean writable;

	public ByteArrayChannel(byte[] array, int offset, int size, boolean writeable) {
		this.array = array;
		this.offset = offset;
		this.size = size;
		this.position = 0;
		this.writable = writeable;
	}

	public ByteArrayChannel(byte[] array, boolean writeable) {
		this(array, 0, array.length, writeable);
	}

	@Override
	public int read(ByteBuffer byteBuffer) {
		if (position() == size()) return -1;
		int bytesPut = (int) Math.min(byteBuffer.remaining(), size() - position());
		byteBuffer.put(array, (int) position() + offset, bytesPut);
		position(position() + bytesPut);
		return bytesPut;
	}

	@Override
	public int write(ByteBuffer byteBuffer) {
		if (!writable) throw new NonWritableChannelException();
		if (position() == size()) return 0;
		int bytesGot = (int) Math.min(byteBuffer.remaining(), size() - position());
		byteBuffer.get(array, (int) position() + offset, bytesGot);
		position(position() + bytesGot);
		return bytesGot;
	}

	@Override
	public long position() {
		return position;
	}

	@Override
	public SeekableByteChannel position(long l) {
		position += (int) l;
		return this;
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public SeekableByteChannel truncate(long l) throws IOException {
		throw new IOException();
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public void close() { }
}
