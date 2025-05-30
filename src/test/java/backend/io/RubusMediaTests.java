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

package backend.io;

import java.nio.file.Path;

public class RubusMediaTests extends MediaTests {
	@Override
	Media getMedia() {
		return new RubusMedia(
			new byte[] { (byte) 0xab},
			"Original Title",
			10,
			1920,
			1080,
			"mp4",
			"mp3",
			"H.264",
			"mp3",
			Path.of("/tmp/path")
		);
	}

	@Override
	Media getDifferentMedia() {
		return new RubusMedia(
			new byte[] { (byte) 0xcd },
			"Another Original Title",
			20,
			1600,
			900,
			"mkv",
			"flac",
			"AV1",
			"flac",
			Path.of("/media/vids")
		);
	}
}
