/*
 * Rubus is an application level protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024-2025 Yegore Vlussove
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

/**
 * MediaPool is responsible for retrieval of all available media and their meta-information.
 */
public class MediaPool {

	private static String dbLocation = "db";

	/**
	 * Returns an array containing all available media and their meta-information.
	 * @return an array containing all available media and their meta-information
	 * @throws IOException if some I/O error occurs
	 */
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

	/**
	 * Same as {@link #availableMedia()} but potentially perform the retrieval faster because some information will be
	 * retrieved dynamically.
	 * @return an array containing all available media
	 * @throws IOException if some I/O error occurs
	 */
	public static Media[] availableMediaFast() throws IOException {
		return availableMedia();
	}

	/**
	 * Returns the meta-information about the specified media.
	 * @param mediaId the id of the media
	 * @return the meta-information
	 * @throws IOException if some I/O occurs
	 */
	public static Media getMedia(String mediaId) throws IOException {
		for (Media media: availableMedia()) {
			if (media.getID().equals(mediaId)) {
				return media;
			}
		}
		return null;
	}

	/**
	 * Returns the location to the file that stores the information about media.
	 * @return the location to the file that stores the information about media
	 */
	public static String getDBLocation() {
		return dbLocation;
	}

	/**
	 * Sets a new location to the file containing the information about media.
	 * @param newDBLocation a new location to the file containing the information about media
	 */
	public static void setDBLocation(String newDBLocation) {
		assert newDBLocation != null;

		dbLocation = newDBLocation;
	}
}
