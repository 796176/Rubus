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

package frontend.decoders;

import org.junit.jupiter.api.*;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public abstract class VideoDecoderTests {

	abstract VideoDecoder getInstance();

	static Path resources = Path.of(System.getProperty("user.dir"), "src", "test", "resources");

	static byte[] videoSample;

	final int frame_rate = 24;

	@BeforeAll
	static void beforeAll() throws IOException {
		videoSample = Files.readAllBytes(Path.of(resources.toString(), "media", "v0.mp4"));
	}

	@Test
	void videoDecoderLifecycleTest() {
		VideoDecoder videoDecoder = getInstance();
		assertNotNull(videoDecoder, "Video decoder failed to instantiate");
		assertDoesNotThrow(getInstance()::close, "Video decoder failed to close");
	}

	@Nested
	class VideoDecoderInstantiated {

		VideoDecoder vd;

		@BeforeEach
		void beforeEach() {
			vd = getInstance();
		}

		@AfterEach
		void afterEach() throws Exception {
			vd.close();
		}

		@Test
		void streamContextLifecycleTest() throws InterruptedException {
			vd.startStreamContextInitialization(videoSample);
			Decoder.StreamContext streamContext = vd.getStreamContextNow();
			assertNull(vd.getStreamContextInitializationException(), "Exception occurred while initializing");
			assertNotNull(streamContext, "Stream context failed to initialize");
			assertDoesNotThrow(streamContext::close, "Stream context failed to close");
			assertTrue(streamContext.isClosed(), "Stream context failed to update status");
		}

		@Nested
		class StreamContextInitialized {

			Decoder.StreamContext sc;

			@BeforeEach
			void beforeEach() throws InterruptedException {
				vd.startStreamContextInitialization(videoSample);
				sc = vd.getStreamContextNow();
			}

			@AfterEach
			void afterEach() throws Exception {
				sc.close();
			}

			@Test
			void retrieveFrameRate() {
				assertEquals(frame_rate, vd.getFrameRate(sc));
			}

			@Test
			void localContextLifecycleTest() throws InterruptedException {
				boolean streamContextInitialOpenStatus = sc.isClosed();
				vd.startLocalContextInitialization(videoSample, sc);
				Decoder.LocalContext localContext = vd.getLocalContextNow();

				assertNull(
					vd.getLocalContextInitializationException(), "Exception occurred while initializing"
				);
				assertNotNull(localContext, "Local context failed to initialize");
				assertDoesNotThrow(localContext::close, "Local context failed to close");
				assertTrue(localContext.isClosed(), "Local context failed to update status");
				assertEquals(streamContextInitialOpenStatus, sc.isClosed(), "Stream context changed status");
			}

			@Nested
			class VideoDecoded {

				VideoDecoder.DecodedFrames decodedFrames;

				@BeforeEach
				void beforeEach() throws InterruptedException{
					vd.startDecodingOfAllFrames(0, sc, videoSample);
					decodedFrames = vd.getDecodedFramesNow(0);
				}

				@Test
				void decodingDidNotThrowException() {
					assertNull(vd.getDecodingException(0));
				}

				@Test
				void amountOfDecodedFramesSatisfied() {
					assertEquals(0, decodedFrames.offset(), "Some frames were skipped");
					assertEquals(frame_rate, decodedFrames.frames().length, "Not all frames decoded");
				}

				@Test
				void allFramesAreNotNull() {
					for (Image frame: decodedFrames.frames()) {
						assertNotNull(frame);
					}
				}

				@Test
				void freeDecodedFrames() {
					vd.freeDecodedFrames(0);
					assertNull(vd.getDecodedFrames(0));
				}

				@Nested
				class VideoDecoderIsPurged {

					@BeforeEach
					void beforeEach() {
						vd.purge();
					}

					@Test
					void decodedFramesPurged() {
						assertNull(vd.getDecodedFrames(0));
					}

					@Test
					void streamContextPurged() {
						assertNull(vd.getStreamContext());
					}
				}
			}

			@Nested
			class LocalContextInitialized {

				Decoder.LocalContext lc;

				@BeforeEach
				void beforeEach() throws InterruptedException {
					vd.startLocalContextInitialization(videoSample, sc);
					lc = vd.getLocalContextNow();
				}

				@Nested
				class DecodeFirstHalf {

					VideoDecoder.DecodedFrames decodedFrames;

					@BeforeEach
					void beforeEach() throws InterruptedException {
						vd.startDecodingOfNFrames(0, lc, videoSample, 0, frame_rate / 2);
						decodedFrames = vd.getDecodedFramesNow(0);
					}

					@Test
					void amountOfDecodedFramesSatisfied() {
						assertEquals(
							0,
							decodedFrames.offset(),
							"First " + decodedFrames.offset() + " frames were skipped"
						);
						assertTrue(
							decodedFrames.frames().length >= frame_rate / 2,
							"Decoded less frames than expected"
						);
					}

					@Test
					void decodedFramesWithinRangeAreNotNull() {
						for (int i = 0; i < frame_rate / 2; i++) {
							assertNotNull(decodedFrames.frames()[i]);
						}
					}
				}

				@Nested
				class DecodeSecondHalf {

					VideoDecoder.DecodedFrames decodedFrames;

					@BeforeEach
					void beforeEach() throws InterruptedException {
						vd.startDecodingOfNFrames(0, lc, videoSample, frame_rate / 2, frame_rate / 2);
						decodedFrames = vd.getDecodedFramesNow(0);
					}

					@Test
					void amountOfDecodedFramesSatisfied() {
						int amount = frame_rate / 2;
						int offset = frame_rate / 2;
						assertTrue(
							decodedFrames.offset() <= offset,
							"First " + (decodedFrames.offset() - offset) + " frames were skipped"
						);
						assertTrue(
							decodedFrames.frames().length - (offset - decodedFrames.offset()) >= amount,
							"Decoded less frames than expected"
						);
					}

					@Test
					void decodedFramesWithinRangeAreNotNull() {
						int offset = decodedFrames.offset();
						for (int i = frame_rate / 2 - offset; i < frame_rate - offset; i++) {
							assertNotNull(decodedFrames.frames()[i]);
						}
					}
				}

				@Nested
				class VideoDecoderPurged {

					@BeforeEach
					void beforeEach() {
						vd.purge();
					}

					@Test
					void localContextPurged() {
						assertNull(vd.getLocalContext());
					}

					@Test
					void streamContextPurged() {
						assertNull(vd.getStreamContext());
					}
				}
			}
		}
	}
}
