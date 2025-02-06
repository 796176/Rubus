/*
 * Rubus is an application level protocol for video and audio streaming and
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MediaPool {

	private static String dbLocation = "db";

	public static Media[] availableMedia() throws IOException {
		return Files.readAllLines(Path.of(getDBLocation())).stream().map(line -> {
			String[] parameters = line.split("\u001e");
			return new RubusMedia(
				parameters[0],
				parameters[1],
				Integer.parseInt(parameters[2]),
				Integer.parseInt(parameters[3]),
				Integer.parseInt(parameters[4]),
				parameters[5],
				parameters[6],
				parameters[7],
				parameters[8],
				Path.of(parameters[9])
			);
		}).toArray(Media[]::new);
	}

	public static Media[] availableMediaFast() throws IOException {
		return availableMedia();
	}

	public static Media getMedia(String mediaId) throws IOException {
		for (Media media: availableMedia()) {
			if (media.getID().equals(mediaId)) {
				return media;
			}
		}
		return null;
	}

	public static String getDBLocation() {
		return dbLocation;
	}

	public static void setDBLocation(String newDBLocation) {
		assert newDBLocation != null;

		dbLocation = newDBLocation;
	}
}
