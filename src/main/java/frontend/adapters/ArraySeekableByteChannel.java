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

package frontend.adapters;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * A concrete implementation of {@link SeekableByteChannel} backed by a byte array. Modifications of the array
 * are visible to this class and its users.
 */
public class ArraySeekableByteChannel implements SeekableByteChannel {

	private final Logger logger = LoggerFactory.getLogger(ArraySeekableByteChannel.class);

	private final byte[] underlyingArray;

	private final int underlyingArrayOffset;

	private final int underlyingArrayLength;

	private int position = 0;

	private volatile boolean isOpen = true;

	/**
	 * Constructs an instance of this class.
	 * @param array the source data for this channel
	 * @param offset the index of the first array element this channel can access
	 * @param length the available length of the array
	 */
	public ArraySeekableByteChannel(@Nonnull byte[] array, int offset, int length) {
		assert offset >= 0 && offset + length <= array.length;

		underlyingArray = array;
		underlyingArrayOffset = offset;
		underlyingArrayLength = length;

		logger.debug("{} instantiated, array size: {}, offset: {}, length: {}", this, array.length, offset, length);
	}

	/**
	 * Constructs an instance of this class.
	 * @param array the source data for this channel
	 */
	public ArraySeekableByteChannel(@Nonnull byte[] array) {
		this(array, 0, array.length);
	}

	@Override
	public synchronized int read(ByteBuffer byteBuffer) throws IOException {
		if (byteBuffer == null) throw new NullPointerException();
		if (!isOpen()) throw new ClosedChannelException();
		if (byteBuffer.isReadOnly()) throw new IllegalArgumentException();
		if (byteBuffer.remaining() == 0) return 0;
		if (position() >= size()) return -1;

		int byteRead = (int) Math.min(byteBuffer.remaining(), size() - position());
		byteBuffer.put(underlyingArray, underlyingArrayOffset + (int) position(), byteRead);
		position(position() + byteRead);
		return byteRead;
	}

	@Override
	public int write(ByteBuffer byteBuffer) throws IOException {
		throw new NonWritableChannelException();
	}

	@Override
	public long position() throws IOException{
		if (!isOpen()) throw new ClosedChannelException();
		return position;
	}

	@Override
	public SeekableByteChannel position(long l) throws IOException {
		int newPosition = (int) l;
		if (!isOpen()) throw new ClosedChannelException();
		if (newPosition < 0) throw new IllegalArgumentException();
		position = newPosition;
		return this;
	}

	@Override
	public long size() throws IOException {
		if (!isOpen()) throw new ClosedChannelException();
		return underlyingArrayLength;
	}

	@Override
	public SeekableByteChannel truncate(long l) {
		throw new NonWritableChannelException();
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public synchronized void close() {
		if (isOpen()) {
			isOpen = false;
			logger.debug("{} closed", this);
		}
	}
}

