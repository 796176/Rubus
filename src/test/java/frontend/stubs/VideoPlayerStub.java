/*
 * Rubus is an application layer protocol for video and audio streaming and
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

package frontend.stubs;

import frontend.exceptions.NotImplementedExceptions;
import frontend.models.EncodedPlaybackClip;
import frontend.interactors.Observer;
import frontend.interactors.PlayerInterface;
import frontend.decoders.VideoDecoder;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VideoPlayerStub implements PlayerInterface {

	public boolean isPaused = false;

	public int progress = 0;

	public int duration = 10;

	public volatile EncodedPlaybackClip[] buffer = new EncodedPlaybackClip[0];

	public boolean isBuffering = true;

	public EncodedPlaybackClip playbackClip = null;

	public ArrayList<Observer> observers = new ArrayList<>();


	public Runnable pauseRunnable = () -> { isPaused = true; };

	public Runnable resumeRunnable = () -> { isPaused = false; };

	public Supplier<Boolean> isPausedSupplier = () -> isPaused;

	public Supplier<Integer> getProgressSupplier = () -> progress;

	public Consumer<Integer> setProgressConsumer = i -> { progress = i; };

	public Supplier<Integer> getVideoDurationSupplier = () -> duration;

	public Consumer<Integer> setVideoDurationSupplier = i -> { duration = i; };

	public Supplier<EncodedPlaybackClip[]> getBufferSupplier = () -> buffer;

	public Consumer<EncodedPlaybackClip[]> getBufferConsumer = b -> { buffer = b; };

	public Consumer<Observer> attachConsumer = o -> { observers.add(o); };

	public Consumer<Observer> detachConsumer = o -> { observers.remove(o); };

	public Runnable sendNotificationRunnable = () -> { for (Observer o: observers) o.update(this); };


	@Override
	public void pause() {
		pauseRunnable.run();
	}

	@Override
	public void resume() {
		resumeRunnable.run();
	}

	@Override
	public boolean isPaused() {
		return isPausedSupplier.get();
	}

	@Override
	public int getProgress() {
		return getProgressSupplier.get();
	}

	@Override
	public int getVideoDuration() {
		return getVideoDurationSupplier.get();
	}

	@Override
	public void setVideoDuration(int duration) {
		setVideoDurationSupplier.accept(duration);
	}

	@Override
	public int getVideoWidth() {
		throw new NotImplementedExceptions();
	}

	@Override
	public int getVideoHeight() {
		throw new NotImplementedExceptions();
	}

	@Override
	public void setProgress(int timestamp) {
		setProgressConsumer.accept(timestamp);
	}

	@Override
	public EncodedPlaybackClip[] getBuffer() {
		return getBufferSupplier.get();
	}

	@Override
	public void setBuffer(EncodedPlaybackClip[] buffer) {
		getBufferConsumer.accept(buffer);
	}

	@Override
	public boolean isBuffering() {
		return isBuffering;
	}

	@Override
	public VideoDecoder getDecoder() {
		throw new NotImplementedExceptions();
	}

	@Override
	public void setDecoder(VideoDecoder videoDecoder) {
		throw new NotImplementedExceptions();
	}

	@Override
	public EncodedPlaybackClip getPlayingClip() {
		return playbackClip;
	}

	@Override
	public void purge() {
		throw new NotImplementedExceptions();
	}

	@Override
	public void attach(Observer o) {
		attachConsumer.accept(o);
	}

	@Override
	public void detach(Observer o) {
		detachConsumer.accept(o);
	}

	@Override
	public Observer[] getObservers() {
		return observers.toArray(new Observer[0]);
	}

	@Override
	public void sendNotification() {
		sendNotificationRunnable.run();
	}

	@Override
	public void close() {
		throw new NotImplementedExceptions();
	}
}
