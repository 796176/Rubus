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

import java.util.Arrays;

/**
 * FetchController is responsible for retrieving video and audio from the server and passing them to the video player.
 * It determines what pieces to fetch based on the player's state; how many pieces and how frequently do so based on its
 * internal configuration. The client can set how many pieces the controller has to fetch before starting playing
 * the video, or the object that handles the network exceptions.
 */
public class FetchController implements Observer {

	private RubusSocket socket;

	private String id;

	private int bufferSize = 15;

	private int minPiecesToFetch = 3;

	private BackgroundFetch backgroundFetch = null;

	private ExceptionHandler handler = null;

	/**
	 * Constructs an instance of this class.
	 * @param rubusSocket a network socket
	 * @param playbackId a media id
	 */
	public FetchController(RubusSocket rubusSocket, String playbackId) {
		assert rubusSocket != null && playbackId != null;

		setSocket(rubusSocket);
		setPlaybackId(playbackId);
	}

	@Override
	public void update(Subject s) {
		if (s instanceof PlayerInterface pi) {
			if (
				bufferSize - pi.getBuffer().length >= minPiecesToFetch &&
				pi.getVideoDuration() > pi.getCurrentSecond() + pi.getBuffer().length + 1
			) {
				int nextPieceIndex =
					pi.getCurrentSecond() + pi.getBuffer().length;
				if (pi.getPlayingPiece() != null) nextPieceIndex++;

				if (backgroundFetch != null && backgroundFetch.isAlive()) {
					if (backgroundFetch.getNextPieceIndex() != nextPieceIndex) {
						backgroundFetch.setNextPieceIndex(nextPieceIndex);
					}
				} else {
					backgroundFetch = new BackgroundFetch(pi, nextPieceIndex);
					backgroundFetch.start();
				}
			}
		}
	}

	/**
	 * Sets a new socket.
	 * @param rubusSocket a new socket
	 */
	public void setSocket(RubusSocket rubusSocket) {
		assert rubusSocket != null;

		socket = rubusSocket;
	}

	/**
	 * Returns the current socket.
	 * @return the current socket
	 */
	public RubusSocket getSocket() {
		return socket;
	}

	/**
	 * Sets a new media id.
	 * @param playbackId a new media id
	 */
	public void setPlaybackId(String playbackId) {
		assert playbackId != null;

		id = playbackId;
	}

	/**
	 * Returns the current media id.
	 * @return the current media id
	 */
	public String getPlaybackId() {
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

	private class BackgroundFetch extends Thread {

		private boolean isInterrupted = false;

		private final PlayerInterface player;

		private Exception exception;

		private int nextPieceIndex;
		
		private BackgroundFetch(PlayerInterface playerInterface, int nextPieceIndex) {
			assert playerInterface != null;

			this.nextPieceIndex = nextPieceIndex;
			player = playerInterface;
		}

		public int getNextPieceIndex() {
			return nextPieceIndex;
		}

		public void setNextPieceIndex(int value) {
			nextPieceIndex = value;
		}

		@Override
		public void run() {
			try {
				int localNextPieceIndex;
				EncodedPlaybackPiece[] buffer;
				do {
					localNextPieceIndex = getNextPieceIndex();
					RubusClient rubusClient = new RubusClient(socket);
					int totalPieces =
						Math.min(bufferSize - player.getBuffer().length, player.getVideoDuration() - localNextPieceIndex);
					RubusRequest request = RubusRequest
						.newBuilder()
						.FETCH(id, localNextPieceIndex, totalPieces)
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
				} while (localNextPieceIndex != getNextPieceIndex());
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
