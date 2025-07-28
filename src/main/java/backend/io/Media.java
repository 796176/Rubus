/*
 * Rubus is an application layer protocol for video and audio streaming and
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

import common.net.response.body.MediaInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Media interface provides access to the media-specific information.
 */
public interface Media {

	/**
	 * Returns the media id.
	 * @return the media id
	 * @throws IOException if some I/O error occurs
	 */
	UUID getID() throws IOException;

	/**
	 * Returns the title of the media.
	 * @return the title of the media
	 * @throws IOException if some I/O error occurs
	 */
	String getTitle() throws IOException;

	/**
	 * Returns the duration of the media.
	 * @return the duration of the media
	 * @throws IOException if some I/O error occurs
	 */
	int getDuration() throws IOException;

	/**
	 * Returns the directory that contains this media-specific files.
	 * @return the directory that contains this media-specific files
	 * @throws IOException if some I/O error occurs
	 */
	Path getContentPath() throws IOException;

	/**
	 * Retrieves audio pieces of the specified range. Audio pieces are represented as byte arrays.
	 * @param pieceIndex the index of the first piece
	 * @param number the number of pieces to retrieve
	 * @return an array of audio pieces
	 * @throws IOException if some I/O error occurs
	 */
	byte[][] fetchAudioPieces(int pieceIndex, int number) throws IOException;

	/**
	 * Retrieves video pieces of the specified range. Video pieces are represented as byte arrays.
	 * @param pieceIndex the index of the first piece
	 * @param number the number of pieces to retrieve
	 * @return an array of video pieces
	 * @throws IOException if some I/O error occurs
	 */
	byte[][] fetchVideoPieces(int pieceIndex, int number) throws IOException;

	/**
	 * Convert this Media instance into a {@link MediaInfo} instance.
	 * @return the {@link MediaInfo} instance
	 * @throws IOException if some I/O error occurs
	 */
	MediaInfo toMediaInfo() throws IOException;
}
