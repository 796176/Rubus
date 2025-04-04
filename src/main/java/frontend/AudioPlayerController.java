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

import common.AudioDecodingException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;

/**
 * AudioPlayerController is responsible for synchronization the video and the audio playback between the {@link PlayerInterface}
 * object and the {@link AudioPlayerInterface} object respectively. The controller keeps track of
 * the {@link PlayerInterface}'s state and changes the state of {@link AudioPlayerInterface} accordingly.
 * AudioPlaybackController retrieves the audio pieces to play from {@link PlayerInterface}' buffer. The client can
 * choose the object responsible for exception handling.
 */
public class AudioPlayerController implements Observer {

	private final AudioPlayerInterface audioPlayer;

	private int lastTimestamp = -1;

	private ExceptionHandler handler;

	/**
	 * Constructs an instance of this class.
	 * @param audioPlayer the audio player
	 * @param handler an exception handler
	 */
	public AudioPlayerController(AudioPlayerInterface audioPlayer, ExceptionHandler handler) {
		assert audioPlayer != null;

		this.handler = handler;
		this.audioPlayer = audioPlayer;
	}

	/**
	 * Constructs an instance of this class without an exception handler.
	 * @param audioPlayer the audio player
	 */
	public AudioPlayerController(AudioPlayerInterface audioPlayer) {
		this(audioPlayer, null);
	}

	@Override
	public void update(Subject s) {
		if (s instanceof PlayerInterface videoPlayer) {
			try {
				if (lastTimestamp == -1) lastTimestamp = videoPlayer.getProgress();

				boolean seekingDetected =
					lastTimestamp > videoPlayer.getProgress() ||
					lastTimestamp + 1 < videoPlayer.getProgress();
				if (seekingDetected) audioPlayer.purge();


				if (videoPlayer.isPaused()) audioPlayer.pause();
				else audioPlayer.resume();

				boolean secondLapsed = lastTimestamp + 1 == videoPlayer.getProgress();
				if (!videoPlayer.isBuffering() && (secondLapsed || audioPlayer.getBuffer().isEmpty())) {
					byte[] audio = videoPlayer.getPlayingPiece().audio();
					AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio));
					audioPlayer.getBuffer().add(ais.readAllBytes());
				}
				lastTimestamp = videoPlayer.getProgress();
			} catch (Exception e) {
				if (handler != null) handler.handleException(new AudioDecodingException(e.getMessage()));
			}
		}
	}

	/**
	 * Returns the current exception handler.<br><br>
	 *
	 * The passed exceptions:
	 *     {@link AudioDecodingException} if the controller failed to decode an audio piece
	 * @return the current exception handler
	 */
	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	/**
	 * Sets a new exception handler.<br><br>
	 *
	 * The passed exceptions:
	 *     {@link AudioDecodingException} if the controller failed to decode an audio piece
	 * @param handler a new exception handler
	 */
	public void setExceptionHandler(ExceptionHandler handler) {
		this.handler = handler;
	}
}
