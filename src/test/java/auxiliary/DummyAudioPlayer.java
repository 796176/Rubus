/*
 * Rubus is an application level protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024 Yegore Vlussove
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

package auxiliary;

import frontend.AudioPlayerInterface;
import frontend.ExceptionHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DummyAudioPlayer implements AudioPlayerInterface {

	public ConcurrentLinkedQueue<byte[]> buffer = new ConcurrentLinkedQueue<>();

	public boolean isPaused = false;

	@Override
	public ConcurrentLinkedQueue<byte[]> getBuffer() {
		return buffer;
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
		return null;
	}

	@Override
	public void setExceptionHandler(ExceptionHandler handler) {

	}

	@Override
	public void purge() {
		buffer.clear();
	}

	@Override
	public void terminate() {
		buffer.clear();
	}
}
