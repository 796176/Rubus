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

import common.AudioDecodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;

/**
 * AudioPlayerController links a particular instance of {@link AudioPlayerInterface} to an instance of
 * {@link PlayerInterface}. It tracks the state of the {@link PlayerInterface} instance and changes the state of
 * the {@link AudioPlayerInterface} instance accordingly. It also supplies the {@link AudioPlayerInterface} instance's
 * buffer with audio clips retrieved from the {@link PlayerInterface}.
 */
public class AudioPlayerController implements Observer {

	private final Logger logger = LoggerFactory.getLogger(AudioPlayerController.class);

	private AudioPlayerInterface audioPlayer;

	private int lastTimestamp = -1;

	private ExceptionHandler handler;

	/**
	 * Constructs an instance of this class.
	 * @param audioPlayer the audio player
	 * @param handler the exception handler
	 */
	public AudioPlayerController(AudioPlayerInterface audioPlayer, ExceptionHandler handler) {
		assert audioPlayer != null;

		this.handler = handler;
		this.audioPlayer = audioPlayer;

		logger.debug("{} instantiated, AudioPlayerInterface: {}, ExceptionHandler: {}", this, audioPlayer, handler);
	}

	/**
	 * Constructs an instance of this class without the exception handler.
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
				logger.info("{} encountered exception", this, e);
				if (handler != null) handler.handleException(new AudioDecodingException(e.getMessage()));
			}
		}
	}

	/**
	 * Returns the current exception handler.<br><br>
	 *
	 * Exception classes that get passed to the handler:<br>
	 * &emsp;{@link AudioDecodingException} if the controller failed to decode an audio clip
	 * @return the current exception handler
	 */
	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	/**
	 * Sets a new exception handler.<br><br>
	 *
	 * Exception classes that get passed to the handler:<br>
	 * &emsp;{@link AudioDecodingException} if the controller failed to decode an audio clip
	 * @param handler a new exception handler
	 */
	public void setExceptionHandler(ExceptionHandler handler) {
		this.handler = handler;
	}

	/**
	 * Returns the current audio player.
	 * @return the current audio player
	 */
	public AudioPlayerInterface getAudioPlayer() {
		return audioPlayer;
	}

	/**
	 * Sets a new audio player.
	 * @param newAudioPlayer a new audio player
	 */
	public void setAudioPlayer(AudioPlayerInterface newAudioPlayer) {
		assert newAudioPlayer != null;

		audioPlayer = newAudioPlayer;
	}
}
