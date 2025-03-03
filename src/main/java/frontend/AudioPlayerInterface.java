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

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AudioPlayerInterface allows the client to control an audio player. The audio player contains a buffer containing
 * PCM encoded ( raw ) audio pieces each 1 second long. If the audio player isn't paused an audio piece gets polled
 * from the buffer and played. This process repeats until the buffer isn't empty. The client can choose what object
 * handles the playback related exceptions.
 */
public interface AudioPlayerInterface {

	/**
	 * The buffer containing the audio pieces. The changes made by the client are visible to this audio player.
	 * @return the buffer containing the audio pieces
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
	 * The passed exceptions:
	 *     {@link common.AudioException} if the audio can't be played ( e.g. the audio sinks aren't available etc.)
	 * @return the current exception handler
	 */
	ExceptionHandler getExceptionHandler();

	/**
	 * Sets a new exception handler<br><br>
	 *
	 * The passed exceptions:
	 *     {@link common.AudioException} if the audio can't be played ( e.g. the audio sinks aren't available etc.)
	 * @param handler a new exception handler
	 */
	void setExceptionHandler(ExceptionHandler handler);

	/**
	 * Purges the currently playing and in the buffer audio pieces.
	 */
	void purge();

	/**
	 * Releases the associated resources and purges this audio player.
	 */
	void terminate();
}
