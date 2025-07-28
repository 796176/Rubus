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

package backend;

import auxiliary.DummySocket;
import auxiliary.MediaPoolStub;
import auxiliary.MediaStub;
import auxiliary.RequestParserStrategyStub;
import backend.io.Media;
import backend.io.MediaPool;
import common.net.request.RubusRequestType;
import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;
import common.net.response.body.MediaInfo;
import common.net.response.body.MediaList;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class RequestHandlerTests {

	DummySocket dummy;

	static MediaPool mediaPool;

	static Media[] instantiateMedia() {
		Media[] availableMedia = new Media[2];
		BiFunction<Integer, Integer, byte[][]> videoFetchingStratMedia1 = (i1, i2) -> {
			if (i1 == 0 && i2 == 1) {
				return new byte[][] {{0x00}};
			}
			fail("Didn't expect arguments i1: " + i1 + ", i2: " + i2);
			return null;
		};
		BiFunction<Integer, Integer, byte[][]> audioFetchingStratMedia1 = (i1, i2) -> {
			if (i1 == 0 && i2 == 1) {
				return new byte[][] {{0x01}};
			}
			fail("Didn't expect arguments i1: " + i1 + ", i2: " + i2);
			return null;
		};
		availableMedia[0] = new MediaStub(
			UUID.fromString("00000000-0000-4000-b000-000000000000"),
			"Title1",
			1,
			null,
			videoFetchingStratMedia1,
			audioFetchingStratMedia1
		);

		BiFunction<Integer, Integer, byte[][]> videoFetchingStratMedia2 = (i1, i2) -> {
			if (i1 == 0 && i2 == 2) {
				return new byte[][] {{0x00}, {0x01}};
			} else if (i1 == 1 && i2 == 1) {
				return new byte[][] {{0x01}};
			}
			fail("Didn't expect arguments i1: " + i1 + ", i2: " + i2);
			return null;
		};
		BiFunction<Integer, Integer, byte[][]> audioFetchingStratMedia2 = (i1, i2) -> {
			if (i1 == 0 && i2 == 2) {
				return new byte[][] {{0x02}, {0x03}};
			} else if (i1 == 1 && i2 == 1){
				return new byte[][] {{0x03}};
			}
			fail("Didn't expect arguments i1: " + i1 + ", i2: " + i2);
			return null;
		};
		availableMedia[1] = new MediaStub(
			UUID.fromString("11111111-1111-4111-b111-111111111111"),
			"Title2",
			2,
			null,
			videoFetchingStratMedia2,
			audioFetchingStratMedia2
		);

		return availableMedia;
	}

	@BeforeAll
	static void beforeAll() {
		Media[] media = instantiateMedia();
		Function<String, Media[]> searchStrat = query -> switch (query) {
			case "" -> media;
			case "title2" -> new Media[] {media[1]};
			default -> fail("Didn't expect arguments query: " + query);
		};
		mediaPool = new MediaPoolStub(media, searchStrat);
	}

	@BeforeEach
	void beforeEach() {
		dummy = new DummySocket(100_000);
	}

	@Test
	void executionStatusIsSuccess() {
		byte[] request = """
			request-type LIST|
			title-contains |
			body-length 0|
			
			""".replace("|\n", "\n").getBytes();
		dummy.write(request);
		RequestParserStrategy parserStrategy = new RequestParserStrategyStub(
			RubusRequestType.LIST,
			Map.of("title-contains", "")
		);

		new RequestHandler(mediaPool, dummy, parserStrategy, requestHandler -> {
			RequestHandler.Status status = requestHandler.getRequestHandlerStatus();
			assertEquals(RequestHandler.ExecutionStatus.SUCCESS, status.getExecutionStatus());
		}).run();
	}

	@Test
	void handleBadParameter() {
		byte[] request = """
			request-type FETCH
			media-id 00000000-0000-4000-b000-000000000000
			starting-playback-piece a
			total-playback-pieces b
			body-length 0
			
			""".getBytes();
		dummy.write(request);
		RequestParserStrategy parserStrategy = new RequestParserStrategyStub(
			RubusRequestType.FETCH,
			Map.of(
				"media-id", "00000000-0000-4000-b000-000000000000",
				"starting-playback-piece", "a",
				"total-playback-pieces", "b"
			)
		);

		new RequestHandler(mediaPool, dummy, parserStrategy).run();
		byte[] response = new byte[10000];
		int responseLen = dummy.read(response);
		assertTrue(
			new String(response, 0, responseLen).startsWith("response-type " + RubusResponseType.BAD_REQUEST)
		);
	}

	@Nested
	class LIST {

		@Test
		void getAllAvailableMedia() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type LIST|
				title-contains |
				body-length 0|
				|
				""".replace("|\n", "\n").getBytes();
			dummy.write(request);
			RequestParserStrategy parserStrategy = new RequestParserStrategyStub(
				RubusRequestType.LIST,
				Map.of("title-contains", "")
			);

			new RequestHandler(mediaPool, dummy, parserStrategy).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream =
				new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			MediaList ml = (MediaList) new ObjectInputStream(inputStream).readObject();
			assertEquals(2, ml.ids().length, "The number of IDs does not match");
			assertEquals(2, ml.titles().length, "The number of titles does not match");

			String[] expectedIDs =
				new String[]{"00000000-0000-4000-b000-000000000000", "11111111-1111-4111-b111-111111111111"};
			assertArrayEquals(expectedIDs, ml.ids(), "The content of the array of IDs doesn't match");
			String[] expectedTitles = new String[]{"Title1", "Title2"};
			assertArrayEquals(expectedTitles, ml.titles(), "The content of the array of titles doesn't match");
		}

		@Test
		void getMediaByTitle() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type LIST
				title-contains title2
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			RequestParserStrategy parserStrategy = new RequestParserStrategyStub(
				RubusRequestType.LIST,
				Map.of("title-contains", "title2")
			);

			new RequestHandler(mediaPool, dummy, parserStrategy).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream =
				new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			MediaList ml = (MediaList) new ObjectInputStream(inputStream).readObject();
			assertEquals(1, ml.ids().length, "The number of IDs does not match");
			assertEquals(1, ml.titles().length, "The number of titles does not match");

			String[] expectedIDs =
				new String[]{"11111111-1111-4111-b111-111111111111"};
			assertArrayEquals(expectedIDs, ml.ids(), "The content of the array of IDs doesn't match");
			String[] expectedTitles = new String[]{"Title2"};
			assertArrayEquals(expectedTitles, ml.titles(), "The content of the array of titles doesn't match");
		}
	}

	@Nested
	class INFO {

		@Test
		void getMediaInfo() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type INFO
				media-id 11111111-1111-4111-b111-111111111111
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			RequestParserStrategy parserStrategy = new RequestParserStrategyStub(
				RubusRequestType.INFO,
				Map.of("media-id", "11111111-1111-4111-b111-111111111111")
			);

			new RequestHandler(mediaPool, dummy, parserStrategy).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream =
				new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			MediaInfo info = (MediaInfo) new ObjectInputStream(inputStream).readObject();
			assertEquals("11111111-1111-4111-b111-111111111111", info.id(), "The id doesn't match");
			assertEquals("Title2", info.title(), "The title doesn't match");
			assertEquals(2, info.duration(), "The duration doesn't match");
		}
	}

	@Nested
	class FETCH {

		@Test
		void fetchSingleClip() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type FETCH
				media-id 00000000-0000-4000-b000-000000000000
				starting-playback-piece 0
				total-playback-pieces 1
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			RequestParserStrategy parserStrategy = new RequestParserStrategyStub(
				RubusRequestType.FETCH,
				Map.of(
					"media-id", "00000000-0000-4000-b000-000000000000",
					"starting-playback-piece", "0",
					"total-playback-pieces", "1"
				)
			);

			new RequestHandler(mediaPool, dummy, parserStrategy).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream =
				new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			FetchedPieces fetchedPieces = (FetchedPieces) new ObjectInputStream(inputStream).readObject();
			assertEquals(
				"00000000-0000-4000-b000-000000000000", fetchedPieces.id(), "The id doesn't match"
			);
			assertEquals(0, fetchedPieces.startingPieceIndex(), "The clip offset doesn't match");
			assertEquals(1, fetchedPieces.video().length, "The number of video clips doesn't match");
			assertEquals(1, fetchedPieces.audio().length, "The number of audio clips doesn't match");

			byte[][] expectedVideo = new byte[][] {{0x00}};
			assertArrayEquals(expectedVideo, fetchedPieces.video(), "The video content doesn't match");
			byte [][] expectedAudio = new byte[][] {{0x01}};
			assertArrayEquals(expectedAudio, fetchedPieces.audio(), "The audio content doesn't match");
		}

		@Test
		void fetchMultipleClips() throws IOException, ClassNotFoundException{
			byte[] request = """
				request-type FETCH
				media-id 11111111-1111-4111-b111-111111111111
				starting-playback-piece 0
				total-playback-pieces 2
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			RequestParserStrategy parserStrategy = new RequestParserStrategyStub(
				RubusRequestType.FETCH,
				Map.of(
					"media-id", "11111111-1111-4111-b111-111111111111",
					"starting-playback-piece", "0",
					"total-playback-pieces", "2"
				)
			);
			new RequestHandler(mediaPool, dummy, parserStrategy).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream =
				new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			FetchedPieces fetchedPieces = (FetchedPieces) new ObjectInputStream(inputStream).readObject();

			assertEquals(
				"11111111-1111-4111-b111-111111111111", fetchedPieces.id(), "The id doesn't match"
			);
			assertEquals(0, fetchedPieces.startingPieceIndex(), "The clip offset doesn't match");
			assertEquals(2, fetchedPieces.video().length, "The number of video clips doesn't match");
			assertEquals(2, fetchedPieces.audio().length, "The number of audio clips doesn't match");

			byte [][] expectedVideo = new byte[][] {{0x00}, {0x01}};
			assertArrayEquals(expectedVideo, fetchedPieces.video(), "The video content doesn't match");
			byte [][] expectedAudio = new byte[][] {{0x02}, {0x03}};
			assertArrayEquals(expectedAudio, fetchedPieces.audio(), "The audio content doesn't match");
		}

		@Test
		void fetchSingleClipFromTheMiddle() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type FETCH
				media-id 11111111-1111-4111-b111-111111111111
				starting-playback-piece 1
				total-playback-pieces 1
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			RequestParserStrategy parserStrategy = new RequestParserStrategyStub(
				RubusRequestType.FETCH,
				Map.of(
					"media-id", "11111111-1111-4111-b111-111111111111",
					"starting-playback-piece", "1",
					"total-playback-pieces", "1"
				)
			);

			new RequestHandler(mediaPool, dummy, parserStrategy).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream =
				new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			FetchedPieces fetchedPieces = (FetchedPieces) new ObjectInputStream(inputStream).readObject();

			assertEquals(
				"11111111-1111-4111-b111-111111111111", fetchedPieces.id(), "The id doesn't match"
			);
			assertEquals(1, fetchedPieces.startingPieceIndex(), "The clip offset doesn't match");
			assertEquals(1, fetchedPieces.video().length, "The number of video clips doesn't match");
			assertEquals(1, fetchedPieces.audio().length, "The number of audio clips doesn't match");

			byte[][] expectedVideo = new byte[][] {{0x1}};
			assertArrayEquals(expectedVideo, fetchedPieces.video(), "The video content doesn't match");
			byte[][] expectedAudio = new byte[][] {{0x3}};
			assertArrayEquals(expectedAudio, fetchedPieces.audio(), "The audio content doesn't match");
		}
	}

	@Nested
	class FailRequestHandler {

		@Test
		void timeoutException() {
			RequestHandler requestHandler = new RequestHandler(
				mediaPool,
				dummy,
				new RequestParserStrategyStub(null, null),
				rh -> {
					RequestHandler.Status status = rh.getRequestHandlerStatus();
					assertTrue(
						status.getExecutionStatus() == RequestHandler.ExecutionStatus.EXCEPTION &&
							status.getException() instanceof SocketTimeoutException
					);
				}
			);
			requestHandler.run();
		}

		@Test
		void eofException() {
			dummy.close();
			RequestHandler requestHandler = new RequestHandler(
				mediaPool,
				dummy,
				new RequestParserStrategyStub(null, null),
				rh -> {
					RequestHandler.Status status = rh.getRequestHandlerStatus();
					assertTrue(
						status.getExecutionStatus() == RequestHandler.ExecutionStatus.EXCEPTION &&
							status.getException() instanceof EOFException
					);
				}
			);
			requestHandler.run();
		}
	}
}
