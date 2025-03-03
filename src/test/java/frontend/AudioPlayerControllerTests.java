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

import auxiliary.DummyAudioPlayer;
import auxiliary.DummyPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AudioPlayerControllerTests {

	byte[] wavSilence = new byte[] {
		82, 73, 70, 70, 37, 0, 0, 0, 87, 65, 86, 69, 102, 109, 116, 32, 16, 0, 0, 0, 1,
		0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 8, 0, 100, 97, 116, 97, 1, 0, 0, 0, -128
	};

	DummyPlayer videoPlayer;

	DummyAudioPlayer audioPlayer;

	AudioPlayerController controller;

	@BeforeEach
	void beforeEach() {
		videoPlayer = new DummyPlayer();
		audioPlayer = new DummyAudioPlayer();
		controller = new AudioPlayerController(audioPlayer);
	}

	@Nested
	class VideoPaused {

		@BeforeEach
		void beforeEach() {
			videoPlayer.isPaused = true;
			controller.update(videoPlayer);
		}

		@Test
		void audioPauseTest() {
			assertTrue(audioPlayer.isPaused());
		}

		@Test
		void videoResumed() {
			videoPlayer.isPaused = false;
			controller.update(videoPlayer);
			assertFalse(audioPlayer.isPaused());
		}
	}

	@Nested
	class VideoResumed {

		@BeforeEach
		void beforeEach() {
			videoPlayer.isPaused = false;
			controller.update(videoPlayer);
		}

		@Test
		void audioPlaysTest() {
			assertFalse(audioPlayer.isPaused());
		}

		@Test
		void videoPaused() {
			videoPlayer.isPaused = true;
			assertTrue(videoPlayer.isPaused());
		}
	}

	@Nested
	class VideoBuffering {

		@BeforeEach
		void beforeEach() {
			videoPlayer.playingPiece = new EncodedPlaybackPiece(new byte[0], wavSilence);
			controller.update(videoPlayer);
		}

		@Test
		void ignorePlayingPiece() {
			assertTrue(audioPlayer.getBuffer().isEmpty());
		}
	}

	@Nested
	class VideoPlays {

		@BeforeEach
		void beforeEach() {
			videoPlayer.playbackProgress = 1;
			videoPlayer.isBuffering = false;
			videoPlayer.playingPiece = new EncodedPlaybackPiece(new byte[0], wavSilence);
			controller.update(videoPlayer);
		}

		@Test
		void fillAudioBuffer() {
			assertEquals(1, audioPlayer.getBuffer().size());
		}

		@Test
		void secondLapsed() {
			videoPlayer.playbackProgress++;
			audioPlayer.getBuffer().clear();
			controller.update(videoPlayer);
			assertEquals(1, audioPlayer.getBuffer().size());
		}

		@Nested
		class VideoSeeks {

			@BeforeEach
			void beforeEach() {
				videoPlayer.isBuffering = true;
			}

			@Test
			void forwardWithinVideoBuffer() {
				videoPlayer.playbackProgress += 2;
				controller.update(videoPlayer);

				assertTrue(audioPlayer.getBuffer().isEmpty());
			}

			@Test
			void forwardOutsideVideoBuffer() {
				videoPlayer.playbackProgress += 2;
				videoPlayer.playingPiece = null;
				controller.update(videoPlayer);

				assertTrue(audioPlayer.getBuffer().isEmpty());
			}

			@Test
			void backward() {
				videoPlayer.playbackProgress--;
				videoPlayer.playingPiece = null;
				controller.update(videoPlayer);

				assertTrue(audioPlayer.getBuffer().isEmpty());
			}
		}
	}
}
