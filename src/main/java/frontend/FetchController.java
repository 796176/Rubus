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

import common.RubusSocket;
import common.net.FetchingException;
import common.net.RubusException;
import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;

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

	private Supplier<RubusSocket> socketSupplier;

	private String id;

	private int bufferSize = 15;

	private int minPiecesToFetch = 3;

	private BackgroundFetch backgroundFetch = null;

	private ExceptionHandler handler = null;

	private final RubusClient rubusClient;

	/**
	 * Constructs an instance of this class.
	 * @param socketSupplier a socket supplier
	 * @param mediaId a media id
	 */
	public FetchController(Supplier<RubusSocket> socketSupplier, String mediaId) {
		assert socketSupplier != null && mediaId != null;

		setSocketSupplier(socketSupplier);
		setMediaId(mediaId);
		rubusClient = new RubusClient(socketSupplier);
	}

	@Override
	public void update(Subject s) {
		if (s instanceof PlayerInterface pi) {
			int startingPlaybackPiece =
				pi.getProgress() + pi.getBuffer().length;
			if (pi.getPlayingPiece() != null) startingPlaybackPiece++;
			int missingToCompletePlayback = pi.getVideoDuration() - startingPlaybackPiece;
			int missingToFillBuffer = bufferSize - pi.getBuffer().length;
			int totalPlaybackPieces = Math.min(missingToFillBuffer, missingToCompletePlayback);
			boolean fetchingNeeded =
				totalPlaybackPieces > 0 &&
				(totalPlaybackPieces >= minPiecesToFetch || totalPlaybackPieces == missingToCompletePlayback);

			if (fetchingNeeded) {
				boolean backgroundFetchRunning = backgroundFetch != null && backgroundFetch.isAlive();
				if (backgroundFetchRunning) {
					if (backgroundFetch.getStartingPlaybackPiece() != startingPlaybackPiece) {
						backgroundFetch.setStartingPlaybackPiece(startingPlaybackPiece);
						backgroundFetch.setTotalPlaybackPieces(totalPlaybackPieces);
					}
				} else {
					backgroundFetch = new BackgroundFetch(pi, startingPlaybackPiece, totalPlaybackPieces);
					backgroundFetch.start();
				}
			}
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
	}

	private class BackgroundFetch extends Thread {

		private boolean isInterrupted = false;

		private final PlayerInterface player;

		private Exception exception;

		private final int startingPlaybackPiece;

		private final int totalPlaybackPieces;
		
		private BackgroundFetch(PlayerInterface playerInterface, int startingPlaybackPiece, int totalPlaybackPieces) {
			assert playerInterface != null && startingPlaybackPiece >= 0 && totalPlaybackPieces > 0;

			player = playerInterface;
			this.startingPlaybackPiece = startingPlaybackPiece;
			this.totalPlaybackPieces = totalPlaybackPieces;
		}

		public int getStartingPlaybackPiece() {
			return startingPlaybackPiece;
		}

		public int getTotalPlaybackPieces() {
			return totalPlaybackPieces;
		}

		public void setStartingPlaybackPiece(int value) {
			this.startingPlaybackPiece = value;
		}

		public void setTotalPlaybackPieces(int value) {
			this.totalPlaybackPieces = value;
		}

		@Override
		public void run() {
			try {
				int localNextPieceIndex;
				EncodedPlaybackPiece[] buffer;
				do {
					localNextPieceIndex = getStartingPlaybackPiece();
					RubusRequest request = RubusRequest
						.newBuilder()
						.FETCH(id, getStartingPlaybackPiece(), getTotalPlaybackPieces())
						.build();
					RubusResponse response = rubusClient.send(request, Math.max(player.getBuffer().length, minPiecesToFetch) * 1000L);
					if (response.getResponseType() != RubusResponseType.OK) {
						throw new RubusException("Response type: " + response.getResponseType());
					}
					FetchedPieces fetchedPieces = response.FETCH();
					EncodedPlaybackPiece[] encodedPlaybackPieces = new EncodedPlaybackPiece[fetchedPieces.video().length];
					for (int i = 0; i < encodedPlaybackPieces.length; i++) {
						encodedPlaybackPieces[i] = new EncodedPlaybackPiece(fetchedPieces.video()[i], fetchedPieces.audio()[i]);
					}
					buffer = Arrays.copyOf(player.getBuffer(), player.getBuffer().length + encodedPlaybackPieces.length);
					System.arraycopy(encodedPlaybackPieces, 0, buffer, player.getBuffer().length, encodedPlaybackPieces.length);
				} while (localNextPieceIndex != getStartingPlaybackPiece());
				if (!isInterrupted) {
					player.setBuffer(buffer);
					player.sendNotification();
				}
			} catch (RubusException e) {
				if (handler != null) handler.handleException(e);
			} catch (Exception e) {
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
