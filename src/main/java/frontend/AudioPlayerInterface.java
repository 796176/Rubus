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

package frontend;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AudioPlayerInterface provides methods to control audio playback. Every instance of AudioPlayerInterface has a buffer
 * associated with it. The buffer stores 1 second long PCM encoded audio clips represented as byte arrays. The buffer is
 * available via {@link #getBuffer()}. All changes made in the returned buffer are visible to the instance. When the
 * currently playing audio clip is exhausted, AudioPlayerInterface retrieves the next one from the buffer
 * ( if available ) and starts playing it.
 */
public interface AudioPlayerInterface {

	/**
	 * The buffer containing audio clips. All changes made in the returned buffer are visible to the instance.
	 * @return the buffer containing the audio clips
	 */
	ConcurrentLinkedQueue<byte[]> getBuffer();

	/**
	 * Pauses the audio playback.
	 */
	void pause();

	/**
	 * Resumes the audio playback.
	 */
	void resume();

	/**
	 * Returns true if the audio playback is paused, false otherwise.
	 * @return true if the audio playback is paused, false otherwise
	 */
	boolean isPaused();

	/**
	 * Returns the current exception handler.<br<br>
	 *
	 * Exception classes that get passed to the handler:<br>
	 * &emsp;{@link common.AudioException} if the audio can't be played ( e.g. the audio sinks aren't available, etc.)
	 * @return the current exception handler
	 */
	ExceptionHandler getExceptionHandler();

	/**
	 * Sets a new exception handler.<br><br>
	 *
	 * Exception classes that get passed to the handler:<br>
	 * &emsp;{@link common.AudioException} if the audio can't be played ( e.g. the audio sinks aren't available, etc.)
	 * @param handler a new exception handler
	 */
	void setExceptionHandler(ExceptionHandler handler);

	/**
	 * Removes all the audio clips from the buffer and exhausts the currently playing audio clip.
	 */
	void purge();

	/**
	 * Releases the associated resources and purges this audio player.
	 */
	void terminate();
}
