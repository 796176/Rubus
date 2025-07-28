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
import java.util.UUID;

public class RubusMediaTests extends MediaTests {
	@Override
	Media getMedia() {
		return new RubusMedia(
			UUID.fromString("00000000-0000-4000-b000-000000000000"),
			"Original Title",
			10,
			Path.of("/tmp/path")
		);
	}

	@Override
	Media getDifferentMedia() {
		return new RubusMedia(
			UUID.fromString("11111111-1111-4111-b111-111111111111"),
			"Another Original Title",
			20,
			Path.of("/media/vids")
		);
	}
}
