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

import frontend.*;
import frontend.decoders.VideoDecoder;

import java.util.ArrayList;

public class DummyPlayer implements PlayerInterface, Subject {
	public boolean isPaused = false;

	public EncodedPlaybackPiece[] buffer = new EncodedPlaybackPiece[0];

	public EncodedPlaybackPiece playingPiece = null;

	public boolean isBuffering = true;

	public int playbackProgress = 0;

	public int videoDuration = 10;

	public int videoWidth = 1600;

	public int videoHeight = 1600;

	public VideoDecoder decoder = null;

	public ArrayList<Observer> observers = new ArrayList<>();

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
	public int getProgress() {
		return playbackProgress;
	}

	@Override
	public int getVideoDuration() {
		return videoDuration;
	}

	@Override
	public void setVideoDuration(int duration) {
		videoDuration = duration;
	}

	@Override
	public int getVideoWidth() {
		return videoWidth;
	}

	@Override
	public int getVideoHeight() {
		return videoHeight;
	}

	@Override
	public void setProgress(int timestamp) {
		playbackProgress = timestamp;
	}

	@Override
	public EncodedPlaybackPiece[] getBuffer() {
		return buffer;
	}

	@Override
	public void setBuffer(EncodedPlaybackPiece[] buffer) {
		assert buffer != null;

		this.buffer = buffer;
	}

	@Override
	public boolean isBuffering() {
		return isBuffering;
	}

	@Override
	public VideoDecoder getDecoder() {
		return decoder;
	}

	@Override
	public void setDecoder(VideoDecoder videoDecoder) {
		decoder = videoDecoder;
	}

	@Override
	public EncodedPlaybackPiece getPlayingPiece() {
		return playingPiece;
	}

	@Override
	public void purge() { }

	@Override
	public void attach(Observer o) {
		assert o != null;

		observers.add(o);
	}

	@Override
	public void detach(Observer o) {
		observers.remove(o);
	}

	@Override
	public Observer[] getObservers() {
		return observers.toArray(new Observer[0]);
	}

	@Override
	public void sendNotification() {
		for (Observer o: observers) {
			o.update(this);
		}
	}

	@Override
	public void close() { }
}
