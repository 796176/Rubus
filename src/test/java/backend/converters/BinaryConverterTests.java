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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BinaryConverterTests<T> {

	public abstract T getModel();

	public abstract BinaryConverter<T> getConverter();

	public abstract boolean testEquality(T m1, T m2);

	@Test
	void conversionTest() throws IOException {
		BinaryConverter<T> converter = getConverter();
		try (SeekableByteChannel conversionOutput = converter.convert(getModel())) {
			T transConvertedModel = converter.convert(conversionOutput);
			assertTrue(testEquality(getModel(), transConvertedModel));
		}
	}
}
