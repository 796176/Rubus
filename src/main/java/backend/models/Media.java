/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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

package backend.models;

import jakarta.annotation.Nonnull;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;

/**
 * The Media interface provides access to media-specific information.
 */
public interface Media {

	/**
	 * Returns the media id.
	 * @return the media id
	 */
	@Nonnull
	UUID getID();

	/**
	 * Returns the title of the media.
	 * @return the title of the media
	 */
	@Nonnull
	String getTitle();

	/**
	 * Returns the duration of the media.
	 * @return the duration of the media
	 */
	int getDuration();

	/**
	 * Returns the URI of the media content.
	 * @return the URI of the media content
	 */
	@Nonnull
	URI getContentURI();

	/**
	 * Retrieves audio clips of the specified range.
	 * @param offset how many clips to skip
	 * @param amount how many clips to retrieve
	 * @return an ordered array of {@link SeekableByteChannel} instances containing audio clips
	 */
	@Nonnull
	SeekableByteChannel[] retrieveAudioClips(int offset, int amount);

	/**
	 * Retrieves video clips of the specified range.
	 * @param offset how many clips to skip
	 * @param amount how many clips to retrieve
	 * @return an ordered array of {@link SeekableByteChannel} instances containing video clips
	 */
	@Nonnull
	SeekableByteChannel[] retrieveVideoClips(int offset, int amount);
}
