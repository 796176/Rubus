/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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
import frontend.interactors.ExceptionHandler;
import frontend.models.EncodedPlaybackClip;
import frontend.models.MediaFetch;
import frontend.network.RubusResponseType;
import frontend.stubs.RubusClientStub;
import frontend.stubs.RubusRequestBuilderStub;
import frontend.stubs.RubusResponseStub;
import frontend.stubs.VideoPlayerStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FetchControllerTests {

	VideoPlayerStub videoPlayerStub = new VideoPlayerStub();

	AtomicInteger updateCounter = new AtomicInteger();

	RubusRequestBuilderStub rubusRequestBuilderStub = new RubusRequestBuilderStub();

	RubusClientStub rubusClientStub = new RubusClientStub();

	RubusResponseStub rubusResponseStub = new RubusResponseStub();

	String mediaId = "test_id";

	int bufferSize = 15;

	int minimumBatchSize = 3;

	FetchController controller = new FetchController(() -> rubusClientStub, mediaId, bufferSize, minimumBatchSize);

	Field backgroundFetchField;

	@BeforeEach
	void beforeEach() throws NoSuchFieldException {
		videoPlayerStub.duration = bufferSize;
		videoPlayerStub.sendNotificationRunnable = updateCounter::getAndIncrement;

		rubusClientStub.getRequestBuilderSupplier = () -> rubusRequestBuilderStub;

		backgroundFetchField = controller.getClass().getDeclaredField("backgroundFetch");
		backgroundFetchField.setAccessible(true);
	}

	@AfterEach
	void afterEach() throws IOException {
		controller.close();
	}

	@Nested
	class VideoStartsPlaying {

		@Test
		void fromStart() throws InterruptedException, IllegalAccessException {
			int expectedOffset = videoPlayerStub.getProgress();
			int expectedAmount = videoPlayerStub.getVideoDuration() - expectedOffset;
			rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
				assertEquals(mediaId, id, "The passed id is different");
				assertEquals(expectedOffset, offset, "Unexpected offset value");
				assertEquals(expectedAmount, amount, "Unexpected amount value");
			};
			MediaFetch mediaFetch = new MediaFetch(
				mediaId, expectedOffset, new byte[expectedAmount][0], new byte[expectedAmount][0]
			);
			rubusClientStub.sendFunction = (request, timeout) -> {
				assertSame(
					rubusRequestBuilderStub.rubusRequest,
					request,
					"The passed rubus request is a different object"
				);
				rubusResponseStub.fetchSupplier = () -> mediaFetch;
				return rubusResponseStub;
			};

			controller.update(videoPlayerStub);
			Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
			controllerInnerThread.join();

			assertEquals(
				expectedAmount,
				videoPlayerStub.getBuffer().length,
				"The size of the video player buffer doesn't match"
			);
			assertEquals( 1, updateCounter.get(), "The video player is expected to be notified once");
		}

		@Test
		void fromMiddle() throws InterruptedException, IllegalAccessException {
			videoPlayerStub.progress = 5;
			int expectedOffset = videoPlayerStub.getProgress();
			int expectedAmount = videoPlayerStub.getVideoDuration() - expectedOffset;
			rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
				assertEquals(mediaId, id, "The passed id is different");
				assertEquals(expectedOffset, offset, "Unexpected offset value");
				assertEquals(expectedAmount, amount, "Unexpected amount value");
			};
			MediaFetch mediaFetch = new MediaFetch(
				mediaId, expectedOffset, new byte[expectedAmount][0], new byte[expectedAmount][0]
			);
			rubusClientStub.sendFunction = (request, timeout) -> {
				assertSame(
					rubusRequestBuilderStub.rubusRequest,
					request,
					"The passed rubus request is a different object"
				);
				rubusResponseStub.fetchSupplier = () -> mediaFetch;
				return rubusResponseStub;
			};

			controller.update(videoPlayerStub);
			Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
			controllerInnerThread.join();

			assertEquals(
				expectedAmount,
				videoPlayerStub.getBuffer().length,
				"The size of the video player buffer doesn't match"
			);
			assertEquals(1, updateCounter.get(), "The video player is expected to be notified once");
		}

		@Nested
		class ProgressChangedWhileFetching {

			@BeforeEach
			void beforeEach() {
				videoPlayerStub.progress = 5;
				int initialExceptedOffset = videoPlayerStub.getProgress();
				int initialExceptedAmount = videoPlayerStub.getVideoDuration() - initialExceptedOffset;
				rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
					assertEquals(mediaId, id, "The passed id is different");
					assertEquals(initialExceptedOffset, offset, "Unexpected offset value");
					assertEquals(initialExceptedAmount, amount, "Unexpected amount value");
				};
				CountDownLatch countDownLatch = new CountDownLatch(1);
				rubusClientStub.sendFunction = (request, timeout) -> {
					assertSame(
						rubusRequestBuilderStub.rubusRequest,
						request,
						"The passed rubus request is a different object"
					);
					countDownLatch.await();
					throw new InterruptedException();
				};
				rubusClientStub.closeRunnable = countDownLatch::countDown;

				controller.update(videoPlayerStub);
			}

			@Test
			void progressDecreased() throws IllegalAccessException, InterruptedException {
				videoPlayerStub.progress = 0;
				int updatedExceptedOffset = videoPlayerStub.getProgress();
				int updatedExceptedAmount = videoPlayerStub.getVideoDuration() - updatedExceptedOffset;
				rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
					assertEquals(mediaId, id, "The passed id is different");
					assertEquals(updatedExceptedOffset, offset, "Unexpected offset value");
					assertEquals(updatedExceptedAmount, amount, "Unexpected amount value");
				};
				MediaFetch mediaFetch = new MediaFetch(
					mediaId,
					updatedExceptedOffset,
					new byte[updatedExceptedAmount][0],
					new byte[updatedExceptedAmount][0]
				);
				rubusClientStub.sendFunction = (request, timeout) -> {
					assertSame(
						rubusRequestBuilderStub.rubusRequest,
						request,
						"The passed rubus request is a different object"
					);
					rubusResponseStub.fetchSupplier = () -> mediaFetch;
					return rubusResponseStub;
				};

				controller.update(videoPlayerStub);
				Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
				controllerInnerThread.join();

				assertEquals(
					updatedExceptedAmount,
					videoPlayerStub.getBuffer().length,
					"The size of the video player buffer doesn't match"
				);
				assertEquals(1, updateCounter.get(), "The video player is expected to be notified once");
			}

			@Test
			void progressIncreased() throws IllegalAccessException, InterruptedException {
				videoPlayerStub.progress += 1;
				int updatedExceptedOffset = videoPlayerStub.getProgress();
				int updatedExceptedAmount = videoPlayerStub.getVideoDuration() - updatedExceptedOffset;
				rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
					assertEquals(mediaId, id, "The passed id is different");
					assertEquals(updatedExceptedOffset, offset, "Unexpected offset value");
					assertEquals(updatedExceptedAmount, amount, "Unexpected amount value");
				};
				MediaFetch mediaFetch = new MediaFetch(
					mediaId,
					updatedExceptedOffset,
					new byte[updatedExceptedAmount][0],
					new byte[updatedExceptedAmount][0]
				);
				rubusClientStub.sendFunction = (request, timeout) -> {
					assertSame(
						rubusRequestBuilderStub.rubusRequest,
						request,
						"The passed rubus request is a different object"
					);
					rubusResponseStub.fetchSupplier = () -> mediaFetch;
					return rubusResponseStub;
				};

				controller.update(videoPlayerStub);
				Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
				controllerInnerThread.join();

				assertEquals(
					updatedExceptedAmount,
					videoPlayerStub.getBuffer().length,
					"The size of the video player doesn't match"
				);
				assertEquals(1, updateCounter.get(), "The video player is expected to be notified once");
			}
		}
	}

	@Nested
	class VideoPlaying {

		@BeforeEach
		void beforeEach() {
			videoPlayerStub.isBuffering = false;
			videoPlayerStub.playbackClip = new EncodedPlaybackClip(new byte[0], new byte[0]);
		}

		@Nested
		class EmptyBuffer {

			@Test
			void playingFromStart() throws InterruptedException, IllegalAccessException {
				int expectedOffset = videoPlayerStub.getProgress() + 1;
				int expectedAmount = videoPlayerStub.getVideoDuration() - expectedOffset;
				rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
					assertEquals(mediaId, id, "The passed video id is different");
					assertEquals(expectedOffset, offset, "Unexpected offset value");
					assertEquals(expectedAmount, amount, "Unexpected amount value");
				};
				MediaFetch mediaFetch = new MediaFetch(
					mediaId, expectedOffset, new byte[expectedAmount][0], new byte[expectedAmount][0]
				);
				rubusClientStub.sendFunction = (request, timeout) -> {
					assertSame(
						rubusRequestBuilderStub.rubusRequest,
						request,
						"The passed rubus request is a different object"
					);
					rubusResponseStub.fetchSupplier = () -> mediaFetch;
					return rubusResponseStub;
				};

				controller.update(videoPlayerStub);
				Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
				controllerInnerThread.join();

				assertEquals(
					expectedAmount,
					videoPlayerStub.getBuffer().length,
					"The size of the video player buffer doesn't match"
				);
				assertEquals(1, updateCounter.get(), "The video player is expected to be notified once");
			}

			@Test
			void needsToFetchOneClip() throws InterruptedException, IllegalAccessException {
				videoPlayerStub.progress = videoPlayerStub.getVideoDuration() - 2;
				int expectedOffset = videoPlayerStub.getProgress() + 1;
				int expectedAmount = videoPlayerStub.getVideoDuration() - expectedOffset;
				rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
					assertEquals(id, mediaId, "The passed id is different");
					assertEquals(expectedOffset, offset, "Unexpected offset value");
					assertEquals(expectedAmount, amount, "Unexpected amount value");
				};
				MediaFetch mediaFetch = new MediaFetch(
					mediaId, expectedOffset, new byte[expectedAmount][0], new byte[expectedAmount][0]
				);
				rubusClientStub.sendFunction = (request, timeout) -> {
					assertSame(
						request,
						rubusRequestBuilderStub.rubusRequest,
						"The passed rubus request is a different object"
					);
					rubusResponseStub.fetchSupplier = () -> mediaFetch;
					return rubusResponseStub;
				};

				controller.update(videoPlayerStub);
				Thread innerControllerThread = (Thread) backgroundFetchField.get(controller);
				innerControllerThread.join();

				assertEquals(
					expectedAmount,
					videoPlayerStub.getBuffer().length,
					"The size of the video player buffer doesn't match"
				);
				assertEquals(1, updateCounter.get(), "The video player is expected to be notified once");
			}

			@Test
			void playingLastClip() throws IllegalAccessException {
				videoPlayerStub.progress = videoPlayerStub.getVideoDuration() - 1;

				controller.update(videoPlayerStub);

				Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
				assertTrue(
					controllerInnerThread == null || !controllerInnerThread.isAlive(),
					"Background activity of FetchController detected"
				);
				assertEquals(0, updateCounter.get(), "The video player was unnecessarily notified");
			}
		}

		@Nested
		class BufferFull {

			@Nested
			class PrefetchedEnoughToCompletePlayback {

				@Test
				void playingNextToLastClip() throws IllegalAccessException {
					videoPlayerStub.progress = videoPlayerStub.getVideoDuration() - 2;
					videoPlayerStub.buffer =
						new EncodedPlaybackClip[]{new EncodedPlaybackClip(new byte[0], new byte[0])};

					controller.update(videoPlayerStub);

					Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
					assertTrue(
						controllerInnerThread == null || !controllerInnerThread.isAlive(),
						"Background activity of FetchController detected"
					);
					assertEquals(0, updateCounter.get(), "The video player was unnecessarily notified");
				}

				@Test
				void playingFromStart() throws IllegalAccessException {
					videoPlayerStub.duration = bufferSize + 1;
					videoPlayerStub.buffer = new EncodedPlaybackClip[bufferSize];
					Arrays.setAll(videoPlayerStub.buffer, i -> new EncodedPlaybackClip(new byte[0], new byte[0]));

					controller.update(videoPlayerStub);

					Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
					assertTrue(
						controllerInnerThread == null || !controllerInnerThread.isAlive(),
						"Background activity of FetchController detected"
					);
					assertEquals(0, updateCounter.get(), "The video player was unnecessarily notified");
				}
			}

			@Nested
			class LacksSomeToCompletePlayback {

				@Test
				void playingFromStart() throws IllegalAccessException {
					videoPlayerStub.duration = bufferSize + 2;
					videoPlayerStub.buffer = new EncodedPlaybackClip[bufferSize];
					Arrays.setAll(videoPlayerStub.getBuffer(), i -> new EncodedPlaybackClip(new byte[0], new byte[0]));

					controller.update(videoPlayerStub);

					Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
					assertTrue(
						controllerInnerThread == null || !controllerInnerThread.isAlive(),
						"Background activity of FetchController detected"
					);
					assertEquals(0, updateCounter.get(), "The video player was unnecessarily notified");
				}

				@Test
				void playingFromMiddle() throws IllegalAccessException {
					videoPlayerStub.progress = 5;
					videoPlayerStub.buffer = new EncodedPlaybackClip[bufferSize];
					Arrays.setAll(videoPlayerStub.getBuffer(), i -> new EncodedPlaybackClip(new byte[0], new byte[0]));
					videoPlayerStub.duration = videoPlayerStub.getProgress() + bufferSize + 2;

					controller.update(videoPlayerStub);

					Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
					assertTrue(
						controllerInnerThread == null || !controllerInnerThread.isAlive(),
						"Background activity of FetchController detected"
					);
					assertEquals(0, updateCounter.get(), "The video player was unnecessarily notified");
				}
			}
		}

		@Nested
		class BufferPartlyFull {

			@Test
			void hasOneClip() throws InterruptedException, IllegalAccessException {
				videoPlayerStub.buffer =
					new EncodedPlaybackClip[] { new EncodedPlaybackClip(new byte[0], new byte[0]) };
				int initialBufferSize = videoPlayerStub.getBuffer().length;
				int expectedOffset = videoPlayerStub.getProgress() + initialBufferSize + 1;
				int expectedAmount = videoPlayerStub.getVideoDuration() - expectedOffset;
				rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
					assertEquals(mediaId, id, "The passed id is different");
					assertEquals(expectedOffset, offset, "Unexpected offset value");
					assertEquals(expectedAmount, amount, "Unexpected amount value");
				};
				MediaFetch mediaFetch = new MediaFetch(
					mediaId, expectedOffset, new byte[expectedAmount][0], new byte[expectedAmount][0]
				);
				rubusClientStub.sendFunction = (request, timeout) -> {
					assertSame(
						rubusRequestBuilderStub.rubusRequest,
						request,
						"The passed rubus request is a different object"
					);
					rubusResponseStub.fetchSupplier = () -> mediaFetch;
					return rubusResponseStub;
				};

				controller.update(videoPlayerStub);
				Thread innerControllerThread = (Thread) backgroundFetchField.get(controller);
				innerControllerThread.join();

				assertEquals(
					expectedAmount + initialBufferSize,
					videoPlayerStub.getBuffer().length,
					"The size of the video player buffer doesn't match"
				);
				assertEquals(1, updateCounter.get(), "The video player is expected to be notified once");
			}

			@Test
			void lacksOneClip() throws InterruptedException, IllegalAccessException {
				videoPlayerStub.duration = bufferSize + 1;
				videoPlayerStub.buffer = new EncodedPlaybackClip[bufferSize - 1];
				Arrays.setAll(videoPlayerStub.buffer, i -> new EncodedPlaybackClip(new byte[0], new byte[0]));
				int initialBufferSize = videoPlayerStub.getBuffer().length;
				int expectedOffset = videoPlayerStub.getProgress() + initialBufferSize + 1;
				int expectedAmount = videoPlayerStub.getVideoDuration() - expectedOffset;
				rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
					assertEquals(mediaId, id, "The passed id is different");
					assertEquals(expectedOffset, offset, "Unexpected offset value");
					assertEquals(expectedAmount, amount, "Unexpected amount value");
				};
				MediaFetch mediaFetch = new MediaFetch(
					mediaId, expectedOffset, new byte[expectedAmount][0], new byte[expectedAmount][0]
				);
				rubusClientStub.sendFunction = (request, timeout) -> {
					assertSame(
						rubusRequestBuilderStub.rubusRequest,
						request,
						"The passed rubus request is a different object"
					);
					rubusResponseStub.fetchSupplier = () -> mediaFetch;
					return rubusResponseStub;
				};

				controller.update(videoPlayerStub);
				Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
				controllerInnerThread.join();

				assertEquals(
					expectedAmount + initialBufferSize,
					videoPlayerStub.getBuffer().length,
					"The size of the video player buffer doesn't match"
				);
				assertEquals(1, updateCounter.get(), "The video player is expected to be notified once");
			}

			@Nested
			class ProgressChangedWhileFetching {

				@Test
				void progressIncreasedWithinPrefetchedBufferRange(
				) throws IllegalAccessException, InterruptedException {
					videoPlayerStub.progress = 5;
					videoPlayerStub.buffer = new EncodedPlaybackClip[3];
					Arrays.setAll(videoPlayerStub.buffer, i -> new EncodedPlaybackClip(new byte[0], new byte[0]));
					int initialBufferSize = videoPlayerStub.getBuffer().length;
					int exceptedOffset = videoPlayerStub.getProgress() + initialBufferSize + 1;
					int exceptedAmount = videoPlayerStub.getVideoDuration() - exceptedOffset;
					rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
						assertEquals(mediaId, id, "The passed id is different");
						assertEquals(exceptedOffset, offset, "Unexpected offset value");
						assertEquals(exceptedAmount, amount, "Unexpected amount value");
					};
					MediaFetch mediaFetch = new MediaFetch(
						mediaId, exceptedOffset, new byte[exceptedAmount][0], new byte[exceptedAmount][0]
					);
					CountDownLatch countDownLatch = new CountDownLatch(1);
					rubusClientStub.sendFunction = (request, timeout) -> {
						assertSame(
							rubusRequestBuilderStub.rubusRequest,
							request,
							"The passed rubus request is a different object"
						);
						countDownLatch.await();
						rubusResponseStub.fetchSupplier = () -> mediaFetch;
						return rubusResponseStub;
					};

					controller.update(videoPlayerStub);

					videoPlayerStub.progress += 1;
					videoPlayerStub.buffer = new EncodedPlaybackClip[initialBufferSize - 1];
					Arrays.setAll(videoPlayerStub.buffer, i -> new EncodedPlaybackClip(new byte[0], new byte[0]));
					int updatedBufferSize = videoPlayerStub.getBuffer().length;

					controller.update(videoPlayerStub);
					countDownLatch.countDown();
					Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
					controllerInnerThread.join();

					assertEquals(
						exceptedAmount + updatedBufferSize,
						videoPlayerStub.getBuffer().length,
						"The size of the video player buffer doesn't match"
					);
					assertEquals(1, updateCounter.get(), "The video player is expected to be notified once");
				}
			}
		}
	}

	@Nested
	class FinishedPlaying {

		@BeforeEach
		void beforeEach() {
			videoPlayerStub.progress = videoPlayerStub.getVideoDuration();
		}

		@Test
		void finished() throws IllegalAccessException {
			controller.update(videoPlayerStub);

			Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
			assertTrue(
				controllerInnerThread == null || !controllerInnerThread.isAlive(),
				"Background activity of FetchController detected"
			);
			assertEquals(0, updateCounter.get(), "The video player was unnecessarily notified");
		}
	}

	@Nested
	class FetchingFails {

		static class ExceptionHandlerMock implements ExceptionHandler {

			private volatile Exception receivedException;

			@Override
			public synchronized void handleException(Exception e) {
				receivedException = e;
			}

			public synchronized Exception getException() {
				return receivedException;
			}
		}

		ExceptionHandlerMock exceptionHandlerMock = new ExceptionHandlerMock();

		@BeforeEach
		void beforeEach() {
			controller.setExceptionHandler(exceptionHandlerMock);
		}

		@Test
		void timeoutException() throws IllegalAccessException, InterruptedException {
			int exceptedOffset = videoPlayerStub.getProgress();
			int exceptedAmount = videoPlayerStub.getVideoDuration() - exceptedOffset;
			rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
				assertEquals(mediaId, id, "The passed id is different");
				assertEquals(exceptedOffset, offset, "Unexpected offset value");
				assertEquals(exceptedAmount, amount, "Unexpected amount value");
			};
			rubusClientStub.sendFunction = (request, timeout) -> {
				assertSame(
					rubusRequestBuilderStub.rubusRequest,
					request,
					"The passed rubus request is a different object"
				);
				throw new InterruptedException();
			};

			controller.update(videoPlayerStub);
			Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
			controllerInnerThread.join();

			assertNotNull(
				exceptionHandlerMock.getException(),
				"The exception didn't occur, or it wasn't passed to the handler"
			);
			assertInstanceOf(
				FetchingException.class, exceptionHandlerMock.getException(), "Exception type mismatch"
			);
		}

		@Test
		void serverError() throws IllegalAccessException, InterruptedException {
			int exceptedOffset = videoPlayerStub.getProgress();
			int exceptedAmount = videoPlayerStub.getVideoDuration() - exceptedOffset;
			rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
				assertEquals(mediaId, id, "The passed id is different");
				assertEquals(exceptedOffset, offset, "Unexpected offset value");
				assertEquals(exceptedAmount, amount, "Unexpected amount value");
			};
			rubusClientStub.sendFunction = (request, timeout) -> {
				assertSame(
					rubusRequestBuilderStub.rubusRequest,
					request,
					"The passed rubus request is a different object"
				);
				rubusResponseStub.getResponseTypeSupplier = () -> RubusResponseType.SERVER_ERROR;
				return rubusResponseStub;
			};

			controller.update(videoPlayerStub);
			Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
			controllerInnerThread.join();

			assertNotNull(
				exceptionHandlerMock.getException(),
				"The exception didn't occur, or it wasn't passed to the handler"
			);
			assertInstanceOf(
				FetchingException.class, exceptionHandlerMock.getException(), "Exception type mismatch"
			);
		}

		@Test
		void badRequest() throws InterruptedException, IllegalAccessException {
			int exceptedOffset = videoPlayerStub.getProgress();
			int exceptedAmount = videoPlayerStub.getVideoDuration() - exceptedOffset;
			rubusRequestBuilderStub.fetchConsumer = (id, offset, amount) -> {
				assertEquals(mediaId, id, "The passed id is different");
				assertEquals(exceptedOffset, offset, "Unexpected offset value");
				assertEquals(exceptedAmount, amount, "Unexpected amount value");
			};
			rubusClientStub.sendFunction = (request, timeout) -> {
				assertSame(
					rubusRequestBuilderStub.rubusRequest,
					request,
					"The passed rubus request is a different object"
				);
				rubusResponseStub.getResponseTypeSupplier = () -> RubusResponseType.BAD_REQUEST;
				return rubusResponseStub;
			};

			controller.update(videoPlayerStub);
			Thread controllerInnerThread = (Thread) backgroundFetchField.get(controller);
			controllerInnerThread.join();

			assertNotNull(
				exceptionHandlerMock.receivedException,
				"The exception didn't occur, or it wasn't passed to the handler"
			);
			assertInstanceOf(
				FetchingException.class, exceptionHandlerMock.getException(), "Exception type mismatch"
			);
		}
	}
}
