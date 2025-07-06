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

import common.RubusSocket;
import common.net.FetchingException;
import common.net.RubusException;
import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * FetchController is responsible for retrieving video and audio from the server and passing them to the video player.
 * It determines what pieces to fetch based on the player's state; how many pieces and how frequently do so based on its
 * internal configuration. The client can set how many pieces the controller has to fetch before starting playing
 * the video, or the object that handles the network exceptions.
 */
public class FetchController implements Observer, AutoCloseable {

	private final Logger logger = LoggerFactory.getLogger(FetchController.class);

	private Supplier<RubusSocket> socketSupplier;

	private String id;

	private int bufferSize;

	private int minimumBatchSize;

	private volatile BackgroundFetch backgroundFetch = null;

	private ExceptionHandler handler = null;

	private final RubusClient rubusClient;

	/**
	 * Constructs an instance of this class.
	 * @param socketSupplier a socket supplier
	 * @param mediaId a media id
	 */
	public FetchController(Supplier<RubusSocket> socketSupplier, String mediaId, int bufferSize, int minimumBatchSize) {
		assert socketSupplier != null && mediaId != null;

		setBufferSize(bufferSize);
		setMinimumBatchSize(minimumBatchSize);
		setSocketSupplier(socketSupplier);
		setMediaId(mediaId);
		rubusClient = new RubusClient(socketSupplier);

		logger.debug(
			"{} instantiated, Supplier: {}, media id: {}, buffer size: {}, minimum batch size: {}",
			this,
			socketSupplier,
			mediaId,
			bufferSize,
			minimumBatchSize
		);
	}

	// To future me,
	// If you think you can improve this method, DON'T; you will regret it. If you think there is a way to make
	// the algorithm clearer than it is right now, you're delusional and conceited. If you think there are bugs, ignore
	// them. If other people claim there are bugs, gaslight them.
	// KEEP THE CURRENT METHOD BY ANY MEANS NECESSARY
	@Override
	public void update(Subject s) {
		try {
			if (s instanceof PlayerInterface pi) {
				int startingPlaybackPiece =
					pi.getProgress() + pi.getBuffer().length;
				if (pi.getPlayingPiece() != null) startingPlaybackPiece++;
				int missingToCompletePlayback = pi.getVideoDuration() - startingPlaybackPiece;
				int missingToFillBuffer = bufferSize - pi.getBuffer().length;
				int totalPlaybackPieces = Math.min(missingToFillBuffer, missingToCompletePlayback);
				// fetching only happens when the buffer is not full and it's missing minPiecesToFetch or more pieces,
				// or when the video is close to the end and it's missing less than minPiecesToFetch pieces.
				boolean fetchingNeeded =
					totalPlaybackPieces > 0 &&
					(totalPlaybackPieces >= minimumBatchSize || totalPlaybackPieces == missingToCompletePlayback);

				if (fetchingNeeded) {
					boolean backgroundFetchRunning = backgroundFetch != null && backgroundFetch.isAlive();
					if (backgroundFetchRunning && backgroundFetch.getStartingPlaybackPiece() == startingPlaybackPiece) {
						return;
					} else if (backgroundFetchRunning) {
						backgroundFetch.interrupt();
						rubusClient.close();
					}
					backgroundFetch = new BackgroundFetch(pi, startingPlaybackPiece, totalPlaybackPieces);
					backgroundFetch.start();
				}
			}
		} catch (IOException e) {
			if (handler != null) handler.handleException(e);
		}
	}

	/**
	 * Sets a new socket supplier. If it's necessary to reinitialize the already created sockets using this socket
	 * supplier, the invocation of {@link #close()} is required.
	 * @param socketSupplier a new socket supplier
	 */
	public void setSocketSupplier(Supplier<RubusSocket> socketSupplier) {
		assert socketSupplier != null;

		this.socketSupplier = socketSupplier;
	}

	/**
	 * Returns the current socket supplier.
	 * @return the current socket supplier
	 */
	public Supplier<RubusSocket> getSocketSupplier() {
		return socketSupplier;
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

	public void setMinimumBatchSize(int newMinimumBatchSize) {
		minimumBatchSize = newMinimumBatchSize;
	}

	public int getMinimumBatchSize() {
		return minimumBatchSize;
	}

	/**
	 * Returns the current exception handler.<br<br>
	 *
	 * The passed exceptions:
	 *     {@link RubusException} if the response type wasn't OK
	 *     {@link FetchingException} if fetching has failed ( e.g. due to network errors etc. )
	 * @return the current exception handler
	 */
	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	/**
	 * Sets a new exception handler.<br><br>
	 *
	 * The passed exceptions:
	 *     {@link RubusException} if the response type wasn't OK
	 *     {@link FetchingException} if fetching has failed ( e.g. due to network errors etc. )
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
	 * Reset the state of this FetchController to the initial state of the instantiation.
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
	}

	private class BackgroundFetch extends Thread {

		private final Logger logger = LoggerFactory.getLogger(BackgroundFetch.class);

		private volatile boolean isInterrupted = false;

		private final PlayerInterface player;

		private Exception exception;

		private final int startingPlaybackPiece;

		private final int totalPlaybackPieces;
		
		private BackgroundFetch(PlayerInterface playerInterface, int startingPlaybackPiece, int totalPlaybackPieces) {
			assert playerInterface != null && startingPlaybackPiece >= 0 && totalPlaybackPieces > 0;

			player = playerInterface;
			this.startingPlaybackPiece = startingPlaybackPiece;
			this.totalPlaybackPieces = totalPlaybackPieces;

			logger.debug(
				"{} instantiated, PlayerInterface: {}, starting playback piece: {}, total playback pieces: {}",
				this,
				playerInterface,
				startingPlaybackPiece,
				totalPlaybackPieces
			);
		}

		public int getStartingPlaybackPiece() {
			return startingPlaybackPiece;
		}

		public int getTotalPlaybackPieces() {
			return totalPlaybackPieces;
		}

		@Override
		public void run() {
			try {
				RubusRequest request = RubusRequest
					.newBuilder()
					.FETCH(id, getStartingPlaybackPiece(), getTotalPlaybackPieces())
					.build();
				RubusResponse response = rubusClient.send(request, Math.max(player.getBuffer().length, minimumBatchSize) * 1000L);
				if (response.getResponseType() != RubusResponseType.OK) {
					throw new RubusException("Response type: " + response.getResponseType());
				}
				FetchedPieces fetchedPieces = response.FETCH();
				EncodedPlaybackPiece[] encodedPlaybackPieces = new EncodedPlaybackPiece[fetchedPieces.video().length];
				for (int i = 0; i < encodedPlaybackPieces.length; i++) {
					encodedPlaybackPieces[i] = new EncodedPlaybackPiece(fetchedPieces.video()[i], fetchedPieces.audio()[i]);
				}
				EncodedPlaybackPiece[] buffer = Arrays.copyOf(player.getBuffer(), player.getBuffer().length + encodedPlaybackPieces.length);
				System.arraycopy(encodedPlaybackPieces, 0, buffer, player.getBuffer().length, encodedPlaybackPieces.length);

				if (!isInterrupted) {
					player.setBuffer(buffer);
					player.sendNotification();
				}
			} catch (RubusException e) {
				logger.info("{} failed to fetch result from server", this, e);
				if (handler != null) handler.handleException(e);
			} catch (Exception e) {
				logger.info("{} failed to fetch result from server", this, e);
				if (handler != null) handler.handleException(new FetchingException(e.getMessage()));
			}
		}

		public void interrupt() {
			isInterrupted = true;
		}

		public Exception getException() {
			return exception;
		}
	}
}
