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

package frontend.interactors;

import frontend.exceptions.AudioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AudioPlayer is the default implementation of {@link AudioPlayerInterface} that uses the audio sink provided by the OS.
 * The audio is played in a separate thread.
 */
public class AudioPlayer implements Runnable, AudioPlayerInterface {

	private final Logger logger = LoggerFactory.getLogger(AudioPlayer.class);

	private SourceDataLine audioOutput = null;

	private volatile boolean isTerminated = false;

	private volatile boolean isPaused = false;

	private ExceptionHandler handler;

	private final AudioFormat audioFormat;

	private final int updateTimeMs = 20;

	private final int framesPerUpdate;

	private final ConcurrentLinkedQueue<byte[]> audioQueue = new ConcurrentLinkedQueue<>();

	private volatile boolean purge = false;

	/**
	 * Constructs an instance of this class and starts a new thread.
	 * @param audioFormat the audio format
	 * @param handler the exception handler
	 */
	public AudioPlayer(AudioFormat audioFormat, ExceptionHandler handler) {
		assert audioFormat != null;

		setExceptionHandler(handler);
		this.audioFormat = audioFormat;
		framesPerUpdate = (int) Math.ceil(audioFormat.getFrameRate() / 1000 * updateTimeMs);
		try {
			audioOutput = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
			audioOutput.open(audioFormat, framesPerUpdate * audioFormat.getFrameSize());
			audioOutput.start();

			Thread thread = new Thread(this);
			thread.start();
		} catch (Exception e) {
			logger.info("{} failed to instantiate", this, e);
			if (getExceptionHandler() != null) {
				getExceptionHandler().handleException(new AudioException(e.getMessage()));
			}
		}

		logger.debug("{} instantiated, AudioFormat: {}, ExceptionHandler: {}", this, audioFormat, handler);
	}

	/**
	 * Constructs an instance of this class without an exception handler and starts a new thread.
	 * @param audioFormat the audio format
	 */
	public AudioPlayer(AudioFormat audioFormat) {
		this(audioFormat, null);
	}

	@Override
	public ConcurrentLinkedQueue<byte[]> getBuffer() {
		return audioQueue;
	}

	@Override
	public void run() {
		while_loop: while (!isTerminated) {
			try {
				byte[] audio = audioQueue.element();
				for (int i = 0; i < audio.length;) {
					if (purge) {
						purge = false;
						continue while_loop;
					}
					if (isPaused()) {
						continue;
					}
					audioOutput.write(
						audio,
						i,
						Math.min(framesPerUpdate * audioFormat.getFrameSize(), audio.length - i)
					);
					i += framesPerUpdate * audioFormat.getFrameSize();
				}
				audioQueue.remove();
			} catch (NoSuchElementException ignored) { }
			catch (Exception e) {
				logger.info("{} encountered exception", this, e);
				if (getExceptionHandler() != null) {
					getExceptionHandler().handleException(new AudioException(e.getMessage()));
				}
			}
		}
	}

	@Override
	public void pause() {
		isPaused = true;
	}

	@Override
	public void resume() {
		isPaused = false;
	}

	@Override
	public boolean isPaused() {
		return isPaused;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	@Override
	public void setExceptionHandler(ExceptionHandler handler) {
		this.handler = handler;
	}

	@Override
	public void purge() {
		audioOutput.flush();
		audioQueue.clear();
		purge = true;
	}

	@Override
	public void terminate() {
		isTerminated = true;
		purge();
		audioOutput.close();

		logger.debug("{} terminated", this);
	}
}
