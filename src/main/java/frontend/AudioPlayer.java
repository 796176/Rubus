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

import common.AudioException;

import javax.sound.sampled.*;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioPlayer implements Runnable, AudioPlayerInterface {

	private SourceDataLine audioOutput = null;

	private boolean isTerminated = false;

	private boolean isPaused = false;

	private ExceptionHandler handler;

	private final AudioFormat audioFormat;

	private final int updateTimeMs = 50;

	private final int framesPerUpdate;

	private final ConcurrentLinkedQueue<byte[]> audioQueue = new ConcurrentLinkedQueue<>();

	private boolean purge = false;

	public AudioPlayer(AudioFormat audioFormat, ExceptionHandler handler) {
		assert audioFormat != null;

		this.handler = handler;
		this.audioFormat = audioFormat;
		framesPerUpdate = (int) Math.ceil(audioFormat.getFrameRate() / 1000 * updateTimeMs);
		try {
			audioOutput = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
			audioOutput.open(audioFormat, (int) audioFormat.getFrameRate());
			audioOutput.start();

			Thread thread = new Thread(this);
			thread.start();
		} catch (Exception e) {
			if (handler != null) handler.handleException(new AudioException(e.getMessage()));
		}
	}

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
					Thread.sleep(updateTimeMs);
				}
				audioQueue.remove();
			} catch (NoSuchElementException | InterruptedException ignored) {}
			catch (Exception e) {
				if (handler != null) handler.handleException(new AudioException(e.getMessage()));
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
	}
}
