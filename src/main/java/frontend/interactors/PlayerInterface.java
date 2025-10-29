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

package frontend.interactors;

import frontend.decoders.VideoDecoder;
import frontend.models.EncodedPlaybackClip;

/**
 * PlayerInterface provides an interface to control video playback. The video data itself is stored in a buffer and
 * represented as {@link EncodedPlaybackClip} objects that store 1 second long video and audio clips.<br>
 * The lifecycle of PlayerInterface should be as follows:<br>
 * 1. When the PlayerInterface instance is instantiated its buffer is empty and the {@link #isBuffering()} value is
 * true.<br>
 * 2. When the buffer gets filled with one or more {@link EncodedPlaybackClip} objects, PlayerInterface retrieves
 * the first {@link EncodedPlaybackClip} from the buffer, and it becomes available via {@link #getPlayingClip()}.
 * PlayerInterface then starts decoding this {@link EncodedPlaybackClip}.<br>
 * 3. When the decoding of the {@link EncodedPlaybackClip} is complete, PlayerInterface starts playing it and
 * the {@link #isBuffering()} value changes to false.<br>
 * 4. After the decoded clip is done playing, which is exactly 1 second later after step 3, the next
 * {@link EncodedPlaybackClip} object is pulled from the buffer and becomes available via {@link #getPlayingClip()}.
 * If the PlayerInterface interface is implemented properly then the video data of this {@link EncodedPlaybackClip}
 * is already decoded at this point and will be played, so the {@link #isBuffering()} value remains false and
 * the progress value is incremented by one. This step repeats until the buffer is fully exhausted.<br><br>
 *
 * The buffer containing {@link EncodedPlaybackClip} objects is available via {@link #getBuffer()}. The buffer can be
 * set via {@link #setBuffer(EncodedPlaybackClip[])}. Note that although any array can be set via
 * {@link #setBuffer(EncodedPlaybackClip[])} it is recommended to append the already in-PlayerInterface
 * {@link EncodedPlaybackClip} objects. This is because a proper implementation is likely to pre-decode several video
 * clips of the {@link EncodedPlaybackClip} objects. So replacing the objects will result in a discrepancy between
 * the content of the buffer and PlayerInterface's playback.<br>
 * PlayerInterface extends {@link Subject}, so any object that changes its state including PlayerInterface itself must
 * send a notification via {@link #sendNotification()} when:<br>
 * 1. The content of the buffer has changed.<br>
 * 2. The progress value has changed.<br>
 * 3. The {@link #isBuffering()} value has changed.<br>
 * 4. The playback was paused or resumed.<br>
 */
public interface PlayerInterface extends Subject, AutoCloseable {

	/**
	 * Pauses the playback.
	 */
	void pause();

	/**
	 * Resumes the playback.
	 */
	void resume();

	/**
	 * Returns true if the playback is paused, false otherwise.
	 * @return true if the playback is paused, false otherwise
	 */
	boolean isPaused();

	/**
	 * Returns the playback progress in seconds.
	 * @return the playback progress in seconds
	 */
	int getProgress();

	/**
	 * Returns the duration of the playback in seconds.
	 * @return the duration of the playback in seconds
	 */
	int getVideoDuration();

	/**
	 * Sets a new duration of the playback in seconds.
	 * @param duration a new duration of the playback in seconds
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
	 * Sets the playback progress in seconds.
	 * @param timestamp a new playback progress in seconds
	 */
	void setProgress(int timestamp);

	/**
	 * Returns the current buffer.
	 * @return the current buffer
	 */
	EncodedPlaybackClip[] getBuffer();

	/**
	 * Sets a new buffer.
	 * @param buffer a new buffer
	 */
	void setBuffer(EncodedPlaybackClip[] buffer);

	/**
	 * Returns true if there is nothing to play or the current clip is still being decoded, false otherwise.
	 * @return true if PlayingInterface isn't actually playing, false otherwise
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
	 * Returns the currently playing {@link EncodedPlaybackClip}.
	 * @return the currently playing {@link EncodedPlaybackClip}
	 */
	EncodedPlaybackClip getPlayingClip();

	/**
	 * Resets the state of this PlayerInterface to the state of the instantiation. There is a few ways
	 * this method can be implemented: (1) resetting involves each and every field of the implemented class, (2) it
	 * involves every field but the collection of {@link Observer}s field. The recommended way to implement this method
	 * is the first one, because it follows the "the state of the instantiation" description.
	 * @throws Exception if the underlying resources cannot be closed
	 */
	void purge() throws Exception;
}
