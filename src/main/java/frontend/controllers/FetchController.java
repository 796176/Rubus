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

package frontend.controllers;

import frontend.exceptions.FetchingException;
import frontend.models.EncodedPlaybackClip;
import frontend.models.MediaFetch;
import frontend.network.RubusClient;
import frontend.network.RubusRequest;
import frontend.network.RubusResponse;
import frontend.network.RubusResponseType;
import frontend.interactors.ExceptionHandler;
import frontend.interactors.Observer;
import frontend.interactors.PlayerInterface;
import frontend.interactors.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * FetchController keeps track of the {@link PlayerInterface} instance, in particular its progress value and the state
 * of the buffer that stores {@link EncodedPlaybackClip}s. If FetchController detects that there is not enough data in
 * the buffer, it retrieves the encoded playback clips from the server and updates the buffer. The amount of playback
 * clips needed before starting the playback is set via {@link #setBufferSize(int)}; The minimum amount of playback
 * clips FetchController retrieves from the server in a single request is specified via
 * {@link #setMinimumBatchSize(int)}.
 */
public class FetchController implements Observer, AutoCloseable {

	private final Logger logger = LoggerFactory.getLogger(FetchController.class);

	private String id;

	private int bufferSize;

	private int minimumBatchSize;

	private volatile BackgroundFetch backgroundFetch = null;

	private ExceptionHandler handler = null;

	private final Supplier<RubusClient> rubusClientSupplier;

	private RubusClient rubusClient;

	/**
	 * Constructs an instance of this class.
	 * @param rubusClientSupplier the supplier of {@link RubusClient} instances
	 * @param mediaId the id of the media the {@link PlayerInterface} instance is playing
	 * @param bufferSize the amount of playback clips FetchController attempts to retrieve from the server when the
	 *                   buffer is empty
	 * @param minimumBatchSize the minimum amount of playback clips FetchController can retrieve from the server
	 */
	public FetchController(
		Supplier<RubusClient> rubusClientSupplier, String mediaId, int bufferSize, int minimumBatchSize
	) {
		assert rubusClientSupplier != null && mediaId != null && bufferSize > 0;

		setBufferSize(bufferSize);
		setMinimumBatchSize(minimumBatchSize);
		setMediaId(mediaId);
		this.rubusClientSupplier = rubusClientSupplier;
		rubusClient = rubusClientSupplier.get();

		logger.debug(
			"{} instantiated, Supplier: {}, media id: {}, buffer size: {}, minimum batch size: {}",
			this,
			rubusClientSupplier,
			mediaId,
			bufferSize,
			minimumBatchSize
		);
	}

	@Override
	public void update(Subject s) {
		try {
			if (s instanceof PlayerInterface pi) {
				int clipOffset = pi.getProgress() + pi.getBuffer().length;
				if (pi.getPlayingClip() != null) clipOffset++;
				int missingToCompletePlayback = pi.getVideoDuration() - clipOffset;
				int missingToFillBuffer = getBufferSize() - pi.getBuffer().length;
				int totalPlaybackClips = Math.min(missingToFillBuffer, missingToCompletePlayback);
				// fetching only happens when the buffer is not full and missing >=minBatchSize clips,
				// or when the video is close to the end and missing <minBatchSize clips.
				boolean fetchingNeeded =
					totalPlaybackClips > 0 &&
					(totalPlaybackClips >= getMinimumBatchSize() || totalPlaybackClips == missingToCompletePlayback);

				if (fetchingNeeded) {
					boolean backgroundFetchRunning = backgroundFetch != null && backgroundFetch.isAlive();
					if (backgroundFetchRunning && backgroundFetch.getRequestedClipOffset() == clipOffset) {
						return;
					} else if (backgroundFetchRunning) {
						backgroundFetch.interrupt();
						rubusClient.close();
						rubusClient = rubusClientSupplier.get();
					}
					backgroundFetch = new BackgroundFetch(pi, clipOffset, totalPlaybackClips);
					backgroundFetch.start();
				}
			}
		} catch (IOException e) {
			if (getExceptionHandler() != null) getExceptionHandler().handleException(e);
		}
	}

	/**
	 * Sets a new media id.
	 * @param mediaId a new media id
	 */
	public void setMediaId(String mediaId) {
		assert mediaId != null;

		id = mediaId;
	}

	/**
	 * Returns the current media id.
	 * @return the current media id
	 */
	public String getMediaId() {
		return id;
	}

	/**
	 * Sets a new buffer size.
	 * @param newSize a new buffer size
	 */
	public void setBufferSize(int newSize) {
		assert newSize > 0;

		bufferSize = newSize;
	}

	/**
	 * Returns the current buffer size.
	 * @return the current buffer size
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * Sets a new minimum batch size.
	 * @param newMinimumBatchSize a new minimum batch size
	 */
	public void setMinimumBatchSize(int newMinimumBatchSize) {
		minimumBatchSize = newMinimumBatchSize;
	}

	/**
	 * Returns the current minimum batch size.
	 * @return the current minimum batch size
	 */
	public int getMinimumBatchSize() {
		return minimumBatchSize;
	}

	/**
	 * Returns the current exception handler.<br<br>
	 *
	 * Exception classes that get passed to the handler:<br>
	 * &emsp;{@link FetchingException} if fetching has failed ( e.g. due to network errors, etc. )
	 * @return the current exception handler
	 */
	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	/**
	 * Sets a new exception handler.<br><br>
	 *
	 * Exception classes that get passed to the handler:<br>
	 * &emsp;{@link FetchingException} if fetching has failed ( e.g. due to network errors, etc. )
	 * @param handler a new exception handler
	 */
	public void setExceptionHandler(ExceptionHandler handler) {
		this.handler = handler;
	}

	@Override
	public void close() throws IOException {
		rubusClient.close();

		logger.debug("{} closed", this);
	}

	/**
	 * Resets the state of this FetchController to the state of the instantiation.
	 * @throws IOException if the underlying resource cannot be closed
	 * @throws InterruptedException if the current thread is interrupted while waiting
	 */
	public void purge() throws IOException, InterruptedException {
		boolean isBackgroundFetchRunning = backgroundFetch != null && backgroundFetch.isAlive();
		if (isBackgroundFetchRunning) {
			backgroundFetch.interrupt();
			backgroundFetch.join();
		}
		backgroundFetch = null;
		rubusClient.close();
		rubusClient = rubusClientSupplier.get();
	}

	private class BackgroundFetch extends Thread {

		private final Logger logger = LoggerFactory.getLogger(BackgroundFetch.class);

		private volatile boolean isInterrupted = false;

		private final PlayerInterface player;

		private final int requestedClipOffset;

		private final int requestedClipAmount;
		
		private BackgroundFetch(PlayerInterface playerInterface, int requestedClipOffset, int requestedClipAmount) {
			assert playerInterface != null && requestedClipOffset >= 0 && requestedClipAmount > 0;

			player = playerInterface;
			this.requestedClipOffset = requestedClipOffset;
			this.requestedClipAmount = requestedClipAmount;

			logger.debug(
				"{} instantiated, PlayerInterface: {}, requestedClipOffset: {}, requestedClipAmount: {}",
				this,
				playerInterface,
				requestedClipOffset,
				requestedClipAmount
			);
		}

		public int getRequestedClipOffset() {
			return requestedClipOffset;
		}

		public int getRequestedClipAmount() {
			return requestedClipAmount;
		}

		@Override
		public void run() {
			try {
				RubusRequest request = rubusClient.getRequestBuilder()
					.FETCH(getMediaId(), getRequestedClipOffset(), getRequestedClipAmount())
					.build();
				RubusResponse response = rubusClient
					.send(request, Math.max(player.getBuffer().length, getMinimumBatchSize()) * 1000L);
				if (response.getResponseType() != RubusResponseType.OK) {
					throw new FetchingException("Response type: " + response.getResponseType());
				}
				MediaFetch mediaFetch = response.FETCH();
				EncodedPlaybackClip[] clips = new EncodedPlaybackClip[mediaFetch.video().length];
				for (int i = 0; i < clips.length; i++) {
					clips[i] = new EncodedPlaybackClip(mediaFetch.video()[i], mediaFetch.audio()[i]);
				}
				EncodedPlaybackClip[] buffer = Arrays.copyOf(
					player.getBuffer(), player.getBuffer().length + clips.length
				);
				System.arraycopy(clips, 0, buffer, player.getBuffer().length, clips.length);

				if (!isInterrupted) {
					player.setBuffer(buffer);
					player.sendNotification();
				}
			} catch (Exception e) {
				logger.info("{} failed to fetch result from server", this, e);
				if (handler != null) handler.handleException(new FetchingException(e.getMessage()));
			}
		}

		public void interrupt() {
			isInterrupted = true;
		}
	}
}
