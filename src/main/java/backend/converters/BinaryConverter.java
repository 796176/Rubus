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

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * BinaryConverter converts models from/into binary format.
 * @param <T> the model
 */
public interface BinaryConverter<T> {

	/**
	 * Converts the model instance into binary.
	 * @param input the model instance
	 * @return a {@link SeekableByteChannel} instance containing the converted model
	 * @throws IOException if some I/O exception occurs
	 */
	@Nonnull
	SeekableByteChannel convert(@Nonnull T input) throws IOException;

	/**
	 * Converts the model from binary.
	 * @param input a {@link SeekableByteChannel} instance containing the converted model
	 * @return the model instance
	 * @throws IOException if some I/O exception occurs
	 */
	@Nonnull
	T convert(@Nonnull SeekableByteChannel input) throws IOException;

}
