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

package backend.converters;

import backend.models.MediaFetch;
import backend.stubs.SeekableByteChannelStub;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.UUID;

public class MediaFetchBinaryConverterTests extends BinaryConverterTests<MediaFetch> {

	@Override
	public MediaFetch getModel() {
		return new MediaFetch(
			UUID.fromString("7993e94c-69c0-44bf-903e-d1c78137943a"),
			2,
			new SeekableByteChannel[]{
				new SeekableByteChannelStub(new byte[] {0, 1, 2, 3}),
				new SeekableByteChannelStub(new byte[] {4, 5, 6, 7})
			},
			new SeekableByteChannel[] {
				new SeekableByteChannelStub(new byte[] {10, 11, 12, 13}),
				new SeekableByteChannelStub(new byte[] {14, 15, 16, 17})
			}
		);
	}

	@Override
	public BinaryConverter<MediaFetch> getConverter() {
		return new MediaFetchBinaryConverter();
	}

	@Override
	public boolean testEquality(MediaFetch m1, MediaFetch m2) {
		boolean preliminary = m1.id().equals(m2.id()) &&
			m1.offset() == m2.offset() &&
			m1.video().length == m2.video().length &&
			m1.audio().length == m2.audio().length;
		if (!preliminary) return false;

		try {
			for (int i = 0; i < m1.video().length; i++) {
				SeekableByteChannel m1Clip = m1.video()[i];
				SeekableByteChannel m2Clip = m2.video()[i];
				if (m1Clip.size() != m2Clip.size()) return false;

				ByteBuffer byteBuffer1 = ByteBuffer.allocate((int) m1Clip.size());
				m1Clip.read(byteBuffer1);
				ByteBuffer byteBuffer2 = ByteBuffer.allocate((int) m2Clip.size());
				m2Clip.read(byteBuffer2);
				if (!Arrays.equals(byteBuffer1.array(), byteBuffer2.array())) return false;
			}
			for (int i = 0; i < m1.audio().length; i++) {
				SeekableByteChannel m1Clip = m1.audio()[i];
				SeekableByteChannel m2Clip = m2.audio()[i];
				if (m1Clip.size() != m2Clip.size()) return false;

				ByteBuffer byteBuffer1 = ByteBuffer.allocate((int) m1Clip.size());
				m1Clip.read(byteBuffer1);
				ByteBuffer byteBuffer2 = ByteBuffer.allocate((int) m2Clip.size());
				m2Clip.read(byteBuffer2);
				if (!Arrays.equals(byteBuffer1.array(), byteBuffer2.array())) return false;
			}
			return true;
		} catch (IOException ioException) {
			throw new RuntimeException(ioException);
		}
	}
}
