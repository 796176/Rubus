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

import common.net.response.body.PlaybackInfo;

import java.io.IOException;

/**
 * An interface to access the information about the media.
 */
public interface Media {

	/**
	 * Returns the media id.
	 * @return the media id
	 * @throws IOException if some I/O error occurs
	 */
	String getID() throws IOException;

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
	 * Returns the width of the video in pixels.
	 * @return the width of the video in pixels
	 * @throws IOException if some I/O error occurs
	 */
	int getVideoWidth() throws IOException;

	/**
	 * Returns the height of the video in pixels.
	 * @return the height of the video in pixels
	 * @throws IOException if some I/O error occurs
	 */
	int getVideoHeight() throws IOException;

	/**
	 * Returns the codec of the video.
	 * @return the codec of the video
	 * @throws IOException if some I/O error occurs
	 */
	String getVideoCodec() throws IOException;

	/**
	 * Returns the codec of the audio.
	 * @return the codec of the audio
	 * @throws IOException if some I/O error occurs
	 */
	String getAudioCodec() throws IOException;

	/**
	 * Returns the container of the video.
	 * @return the container of the video
	 * @throws IOException if some I/O error occurs
	 */
	String getVideoContainer() throws IOException;

	/**
	 * Returns the container of the audio.
	 * @return the container of the audio
	 * @throws IOException if some I/O error occurs
	 */
	String getAudioContainer() throws IOException;

	/**
	 * Retrieve audio pieces of the specified range where each piece is represented as a byte array.
	 * @param pieceIndex the index of the first piece
	 * @param number the number of pieces to retrieve
	 * @return an array of audio pieces
	 * @throws IOException if some I/O error occurs
	 */
	byte[][] fetchAudioPieces(int pieceIndex, int number) throws IOException;

	/**
	 * Retrieve video pieces of the specified range where each piece is represented as a byte array.
	 * @param pieceIndex the index of the first piece
	 * @param number the number of pieces to retrieve
	 * @return an array of video pieces
	 * @throws IOException if some I/O error occurs
	 */
	byte[][] fetchVideoPieces(int pieceIndex, int number) throws IOException;

	/**
	 * Convert this Meta instance into {@link PlaybackInfo}.
	 * @return a {@link PlaybackInfo} instance
	 * @throws IOException if some I/O error occurs
	 */
	PlaybackInfo toPlaybackInfo() throws IOException;
}
