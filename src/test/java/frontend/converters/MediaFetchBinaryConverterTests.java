/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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

package frontend.converters;

import frontend.models.MediaFetch;

import java.util.Arrays;

public class MediaFetchBinaryConverterTests extends BinaryConverterTests<MediaFetch> {

	@Override
	public MediaFetch getModel() {
		return new MediaFetch(
			"abcd",
			3,
			new byte[][] {
				{0, 1, 2, 3},
				{4, 5, 6, 7}
			},
			new byte[][] {
				{10, 11, 12, 13},
				{14, 15, 16, 17}
			}
		);
	}

	@Override
	public BinaryConverter<MediaFetch> getConverter() {
		return new MediaFetchBinaryConverter();
	}

	@Override
	public boolean testEquality(MediaFetch m1, MediaFetch m2) {
		return m1.id().equals(m2.id()) &&
			m1.offset() == m2.offset() &&
			Arrays.deepEquals(m1.video(), m2.video()) &&
			Arrays.deepEquals(m1.audio(), m2.audio());
	}
}
