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

package backend.controllers;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A set of methods to provide basic IO utilities.
 */
public class DataStreams {

	/**
	 * Transfers all available data from input to output. This method blocks until the data is transferred.
	 * @param input the source of the data
	 * @param output the destination of the data
	 * @throws IOException if some I/O exception occurs
	 */
	public static void passData(@Nonnull ReadableByteChannel input, @Nonnull OutputStream output) throws IOException {
		byte[] array = new byte[1024 * 8];
		ByteBuffer buffer = ByteBuffer.wrap(array);
		while (true) {
			int bytesRead = input.read(buffer);
			if (bytesRead == -1) return;
			output.write(array, 0, bytesRead);
			buffer.position(0);
		}
	}
}
