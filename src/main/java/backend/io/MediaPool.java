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
 * MediaPool class allows the client to query information about the available media. It stores a reference to
 * the database containing the meta-information about the available media, and when the client makes a query MediaPool
 * parses the database and returns objects where each object represent a single media. Those objects allow the client
 * to get the meta-information. The client can also invoke the appropriate methods to get the data associated with
 * the media.
 */
public class MediaPool {

	private Path dbPath;

	/**
	 * Creates an instance of this class using the specified database location as the main resource of information.
	 * @param mainDBPath the location to the database
	 */
	public MediaPool(Path mainDBPath) {
		assert mainDBPath != null;

		setMainDBPath(mainDBPath);
	}

	/**
	 * Returns an array containing all the available media represented as {@link Media}.
	 * @return an array containing all the available media represented as {@link Media}
	 * @throws IOException if some I/O error occurs
	 */
	public Media[] availableMedia() throws IOException {
		return Files.readAllLines(getMainDBPath()).stream().map(line -> {
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
	 * Same as {@link #availableMedia} but potentially performs the retrieval faster because it's not necessary refer
	 * to the database.
	 * @return an array containing all the available media represented as {@link Media}
	 * @throws IOException if some I/O error occurs
	 */
	public Media[] availableMediaFast() throws IOException {
		return availableMedia();
	}

	/**
	 * Returns the media represented as {@link Media} with the specified media id.
	 * @param mediaId the id of the media
	 * @return the {@link Media} object
	 * @throws IOException if some I/O occurs
	 */
	public Media getMedia(String mediaId) throws IOException {
		for (Media media: availableMedia()) {
			if (media.getID().equals(mediaId)) {
				return media;
			}
		}
		return null;
	}

	/**
	 * Returns the location to the database.
	 * @return the location to the database
	 */
	public Path getMainDBPath() {
		return dbPath;
	}

	/**
	 * Sets a new location to the database.
	 * @param newMainDBPath a new location to the database
	 */
	public void setMainDBPath(Path newMainDBPath) {
		assert newMainDBPath != null;

		dbPath = newMainDBPath;
	}
}
