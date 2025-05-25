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

package frontend;

import frontend.decoders.VideoDecoder;

/**
 * PlayerInterface allows the client to control a video player and track its current status. A video player contains
 * a buffer, which is an array of {@link EncodedPlaybackPiece} objects. Each piece must contain one second worth of video.
 * If a video player isn't paused and the buffer contains at least one {@link EncodedPlaybackPiece} it takes it out of
 * the buffer and starts decoding it. At this moment the current playing piece is available via {@link #getPlayingPiece()}.
 * After the video piece gets decoded the player actually starts playing the video and at this moment the {@link #isBuffering()},
 * value is false. After the current video piece finished playing, the video player increments the playing progress by one.
 * This process repeats until the buffer is empty. {@link #setBuffer(EncodedPlaybackPiece[])} and {@link #getBuffer()}
 * allow to access the entire video buffer, but it's recommended to use them only to append the current buffer. It's
 * because a video player may still keep references to {@link EncodedPlaybackPiece} objects to perform, for example,
 * pre-decoding. So modifying the data of the buffer may cause no effect on the actual playing content. If it's necessary
 * to flush the current buffer and the currently playing video piece setProgress(getProgress()) may be used.
 */
public interface PlayerInterface extends Subject, AutoCloseable {

	/**
	 * Pauses the video player.
	 */
	void pause();

	/**
	 * Resumes the video player.
	 */
	void resume();

	/**
	 * Returns true if the video player is paused, false otherwise.
	 * @return true if the video player is paused, false otherwise
	 */
	boolean isPaused();

	/**
	 * Returns the playing progress in seconds.
	 * @return the playing progress
	 */
	int getProgress();

	/**
	 * Returns the duration of the video in seconds.
	 * @return the duration of the video
	 */
	int getVideoDuration();

	/**
	 * Sets a new duration of the video in seconds.
	 * @param duration a new duration of the video
	 */
	void setVideoDuration(int duration);

	/**
	 * Returns the video width in pixels.
	 * @return the video width
	 */
	int getVideoWidth();

	/**
	 * Returns the video height in pixels.
	 * @return the video height
	 */
	int getVideoHeight();

	/**
	 * Sets the playing progress.
	 * @param timestamp a new playing progress
	 */
	void setProgress(int timestamp);

	/**
	 * Returns the current video buffer.
	 * @return the current video buffer
	 */
	EncodedPlaybackPiece[] getBuffer();

	/**
	 * Sets a new video buffer.
	 * @param buffer a new video buffer
	 */
	void setBuffer(EncodedPlaybackPiece[] buffer);

	/**
	 * Returns true if there is no video to play or the video is still being decoded, false otherwise.
	 * @return true if the video isn't actually playing, false otherwise
	 */
	boolean isBuffering();

	/**
	 * Returns the current decoder.
	 * @return the current decoder
	 */
	VideoDecoder getDecoder();

	/**
	 * Sets a new decoder
	 * @param videoDecoder a new decoder
	 */
	void setDecoder(VideoDecoder videoDecoder);

	/**
	 * Returns the currently playing {@link EncodedPlaybackPiece}.
	 * @return an {@link EncodedPlaybackPiece} instance
	 */
	EncodedPlaybackPiece getPlayingPiece();

	/**
	 * Reset the state of this PlayerInterface to the initial state of the instantiation. There is a few ways
	 * this method can be implemented: (1) resetting involves each and every field of the implemented class, (2) it
	 * involves every field but the collection of {@link Observer}s field, and, if this is the latter, should those
	 * observers be notified by this method or by the caller. The recommended way to implement this method is
	 * the first one, because it is the least confusing way that follows the description
	 * "the initial state of the instantiation".
	 * @throws Exception if the underlying resources cannot be closed
	 */
	void purge() throws Exception;
}
