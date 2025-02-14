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

package frontend;

import common.RubusSocket;
import common.net.FetchingException;
import common.net.RubusException;
import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;

import java.util.Arrays;

public class FetchController implements Observer {

	private RubusSocket socket;

	private String id;

	private int bufferSize = 15;

	private int minPiecesToFetch = 3;

	private BackgroundFetch backgroundFetch = null;

	private ExceptionHandler handler = null;

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
				if (backgroundFetch != null && backgroundFetch.isAlive()) {
					backgroundFetch.interrupt();
				}
				backgroundFetch = new BackgroundFetch(pi);
				backgroundFetch.start();
			}
		}
	}

	public void setSocket(RubusSocket rubusSocket) {
		assert rubusSocket != null;

		socket = rubusSocket;
	}

	public RubusSocket getSocket() {
		return socket;
	}

	public void setPlaybackId(String playbackId) {
		assert playbackId != null;

		id = playbackId;
	}

	public String getPlaybackId() {
		return id;
	}

	public void setBufferSize(int newSize) {
		assert newSize > 0;

		bufferSize = newSize;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	public void setExceptionHandler(ExceptionHandler handler) {
		this.handler = handler;
	}

	private class BackgroundFetch extends Thread {

		private boolean isInterrupted = false;

		private final PlayerInterface player;

		private Exception exception;
		private BackgroundFetch(PlayerInterface playerInterface) {
			assert playerInterface != null;

			player = playerInterface;
		}

		@Override
		public void run() {
			try {
				RubusClient rubusClient = new RubusClient(socket);
				int nextPieceIndex = player.isBuffering() ?
					player.getCurrentSecond() :
					player.getCurrentSecond() + player.getBuffer().length + 1;
				int totalPieces =
					Math.min(bufferSize - player.getBuffer().length, player.getVideoDuration() - nextPieceIndex);
				RubusRequest request = RubusRequest
					.newBuilder()
					.FETCH()
					.params(
						"id " + id,
						"from " + nextPieceIndex,
						"total " + totalPieces
					)
					.build();
				RubusResponse response = rubusClient.send(request, 15000);
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
				if (!isInterrupted) player.setBuffer(buffer);
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
