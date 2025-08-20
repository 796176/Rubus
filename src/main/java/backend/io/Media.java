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
import java.nio.channels.SeekableByteChannel;
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
	 * Retrieves audio clips of the specified range.
	 * @param clipIndex the index of the first clip
	 * @param number the number of clips to retrieve
	 * @return an ordered array of {@link SeekableByteChannel} instances containing audio
	 * @throws IOException if some I/O error occurs
	 */
	SeekableByteChannel[] retrieveAudioClips(int clipIndex, int number) throws Exception;

	/**
	 * Retrieves video clips of the specified range.
	 * @param clipIndex the index of the first clip
	 * @param number the number of clips to retrieve
	 * @return an ordered array of {@link SeekableByteChannel} instances containing video
	 * @throws IOException if some I/O error occurs
	 */
	SeekableByteChannel[] retrieveVideoClips(int clipIndex, int number) throws Exception;

	/**
	 * Convert this Media instance into a {@link MediaInfo} instance.
	 * @return the {@link MediaInfo} instance
	 * @throws IOException if some I/O error occurs
	 */
	MediaInfo toMediaInfo() throws IOException;
}
