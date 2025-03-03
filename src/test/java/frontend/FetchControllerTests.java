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

import auxiliary.DummyPlayer;
import auxiliary.DummySocket;
import common.net.request.RubusRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class FetchControllerTests {

	DummyPlayer videoPlayer;

	DummySocket dummySocket;

	FetchController controller;

	@BeforeEach
	void beforeEach() {
		videoPlayer = new DummyPlayer();
		dummySocket = new DummySocket(100_000);
		dummySocket.blockReading = true;
		controller = new FetchController(dummySocket, "dummy_id");
	}

	@Nested
	class VideoStartsPlaying {

		@Test
		void fromStart() throws InterruptedException{
			controller.update(videoPlayer);
			Thread.sleep(1000);

			byte[] receive = dummySocket.buffer;
			int len = dummySocket.occupied;
			String request = new String(receive, 0, len);
			assertTrue(request.startsWith("request-type " + RubusRequestType.FETCH), "Wrong request type");
			assertTrue(request.contains("media-id dummy_id"), "Wrong id");
			assertTrue(request.contains("first-playback-piece 0"), "Wrong starting playback piece index");
			assertTrue(request.contains("number-playback-pieces 10"), "Wrong number of playback pieces");
		}

		@Test
		void fromMiddle() throws InterruptedException {
			videoPlayer.playbackProgress = 5;
			controller.update(videoPlayer);
			Thread.sleep(1000);

			byte[] receive = dummySocket.buffer;
			int len = dummySocket.occupied;
			String request = new String(receive, 0, len);
			assertTrue(request.startsWith("request-type " + RubusRequestType.FETCH), "Wrong request type");
			assertTrue(request.contains("media-id dummy_id"), "Wrong id");
			assertTrue(request.contains("first-playback-piece 5"), "Wrong starting playback piece index");
			assertTrue(request.contains("number-playback-pieces 5"), "Wrong number of playback pieces");
		}
	}

	@Nested
	class VideoPlaying {

		@BeforeEach
		void beforeEach() {
			videoPlayer.isBuffering = false;
			videoPlayer.playingPiece = new EncodedPlaybackPiece(new byte[0], new byte[0]);
		}

		@Nested
		class EmptyBuffer {

			@Test
			void playingStart() throws InterruptedException {
				controller.update(videoPlayer);
				Thread.sleep(1000);

				byte[] receive = dummySocket.buffer;
				int len = dummySocket.occupied;
				String request = new String(receive, 0, len);
				assertTrue(request.startsWith("request-type " + RubusRequestType.FETCH), "Wrong request type");
				assertTrue(request.contains("media-id dummy_id"), "Wrong id");
				assertTrue(request.contains("first-playback-piece 1"), "Wrong starting playback piece index");
				assertTrue(request.contains("number-playback-pieces 9"), "Wrong number of playback pieces");
			}

			@Test
			void needsToFetchExtraSecond() throws InterruptedException {
				videoPlayer.playbackProgress = 8;
				controller.update(videoPlayer);
				Thread.sleep(1000);

				byte[] receive = dummySocket.buffer;
				int len = dummySocket.occupied;
				String request = new String(receive, 0, len);
				assertTrue(request.startsWith("request-type " + RubusRequestType.FETCH), "Wrong request type");
				assertTrue(request.contains("media-id dummy_id"), "Wrong id");
				assertTrue(request.contains("first-playback-piece 9"), "Wrong starting playback piece index");
				assertTrue(request.contains("number-playback-pieces 1"), "Wrong number of playback pieces");
			}
		}

		@Nested
		class BufferFull {

			@Test
			void playingNextToLastSecond() throws InterruptedException {
				videoPlayer.playbackProgress = 8;
				videoPlayer.buffer = new EncodedPlaybackPiece[] {new EncodedPlaybackPiece(new byte[0], new byte[0])};
				controller.update(videoPlayer);
				Thread.sleep(1000);

				assertEquals(0, dummySocket.occupied, "Controller sent an unnecessary request");
			}

			@Test
			void playingFirstSecond() throws InterruptedException {
				videoPlayer.buffer = new EncodedPlaybackPiece[9];
				Arrays.fill(videoPlayer.buffer, new EncodedPlaybackPiece(new byte[0], new byte[0]));
				controller.update(videoPlayer);
				Thread.sleep(1000);

				assertEquals(0, dummySocket.occupied, "Controller sent an unnecessary request");
			}
		}

		@Nested
		class BufferPartlyFull {

			@Test
			void hasOnePiece() throws InterruptedException {
				videoPlayer.buffer = new EncodedPlaybackPiece[] {new EncodedPlaybackPiece(new byte[0], new byte[0])};
				controller.update(videoPlayer);
				Thread.sleep(1000);

				byte[] receive = dummySocket.buffer;
				int len = dummySocket.occupied;
				String request = new String(receive, 0, len);
				assertTrue(request.startsWith("request-type " + RubusRequestType.FETCH), "Wrong request type");
				assertTrue(request.contains("media-id dummy_id"), "Wrong id");
				assertTrue(request.contains("first-playback-piece 2"), "Wrong starting playback piece index");
				assertTrue(request.contains("number-playback-pieces 8"), "Wrong number of playback pieces");
			}

			@Test
			void lacksOnePiece() throws InterruptedException {
				videoPlayer.buffer = new EncodedPlaybackPiece[8];
				Arrays.fill(videoPlayer.buffer, new EncodedPlaybackPiece(new byte[0], new byte[0]));
				controller.update(videoPlayer);
				Thread.sleep(1000);

				byte[] receive = dummySocket.buffer;
				int len = dummySocket.occupied;
				String request = new String(receive, 0, len);
				assertTrue(request.startsWith("request-type " + RubusRequestType.FETCH), "Wrong request type");
				assertTrue(request.contains("media-id dummy_id"), "Wrong id");
				assertTrue(request.contains("first-playback-piece 9"), "Wrong starting playback piece index");
				assertTrue(request.contains("number-playback-pieces 1"), "Wrong number of playback pieces");
			}

			@Test
			void hasEnough() throws InterruptedException {
				videoPlayer.videoDuration = 30;
				videoPlayer.buffer = new EncodedPlaybackPiece[20];
				Arrays.fill(videoPlayer.buffer, new EncodedPlaybackPiece(new byte[0], new byte[0]));
				controller.update(videoPlayer);
				Thread.sleep(1000);

				assertEquals(0, dummySocket.occupied, "Controller sent an unnecessary request");
			}
		}

		@Nested
		class FinishedPlaying {

			@BeforeEach
			void beforeEach() {
				videoPlayer.playbackProgress = videoPlayer.videoDuration;
			}

			@BeforeEach
			void finished() throws InterruptedException {
				videoPlayer.playbackProgress = 10;
				controller.update(videoPlayer);
				Thread.sleep(1000);

				assertEquals(0, dummySocket.occupied, "Controller sent an unnecessary request");
			}
		}
	}
}
