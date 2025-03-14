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

package backend;

import auxiliary.DummySocket;
import backend.io.MediaPool;
import common.RubusSocket;
import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;
import common.net.response.body.MediaInfo;
import common.net.response.body.MediaList;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class RequestHandlerTests {

	RubusSocket dummy;

	@BeforeAll
	static void beforeAll() {
		MediaPool.setDBLocation(Path.of(System.getProperty("user.dir"), "src", "test", "resources", "testDB").toString());
	}

	@BeforeEach
	void beforeEach() {
		dummy = new DummySocket(100_000);
	}
	@Test
	void handleNoRequest() {
		ArrayList<RubusSocket> sockets = new ArrayList<>();
		RequestHandler handler = new RequestHandler(dummy, sockets::add, s->{});
		handler.run();
		if (sockets.isEmpty()) fail();
	}

	@Test
	void handleClosing() throws IOException {
		dummy.close();
		ArrayList<RubusSocket> sockets = new ArrayList<>();
		RequestHandler handler = new RequestHandler(dummy, s->{}, sockets::add);
		handler.run();
		if (sockets.isEmpty()) fail();
	}

	@Test
	void handleBadRequestType() throws IOException {
		byte[] request = """
			request-type error
			body-length 0
			
			""".getBytes();
		dummy.write(request);
		new RequestHandler(dummy, s->{}, s->{}).run();

		byte[] response = new byte[10000];
		int responseLen = dummy.read(response);
		assertTrue(new String(response, 0, responseLen).startsWith("response-type " + RubusResponseType.BAD_REQUEST));
	}

	@Test
	void handleBadParameter() throws IOException {
		byte[] request = """
			request-type FETCH
			media-id id1
			starting-playback-piece a
			total-playback-pieces b
			body-length 0
			
			""".getBytes();
		dummy.write(request);
		new RequestHandler(dummy, s->{}, s->{}).run();

		byte[] response = new byte[10000];
		int responseLen = dummy.read(response);
		assertTrue(new String(response, 0, responseLen).startsWith("response-type " + RubusResponseType.BAD_REQUEST));
	}

	@Nested
	class LIST {

		@Test
		void getAllAvailable() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type LIST
				title-contains .+
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			new RequestHandler(dummy, s->{}, s->{}).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			MediaList ml = (MediaList) new ObjectInputStream(inputStream).readObject();
			assertEquals(2, ml.ids().length, "The number of ids does not match");
			assertEquals(2, ml.titles().length, "The number of titles does not match");
			if (ml.ids()[0].equals("id1")) {
				assertArrayEquals(new String[]{"id1", "id2"}, ml.ids(), "The ids are not matching");
				assertArrayEquals(new String[]{"Title1", "Title2"}, ml.titles(), "The titles are not matching");
			} else {
				assertArrayEquals(new String[]{"id2", "id1"}, ml.ids(), "The ids are not matching");
				assertArrayEquals(new String[]{"Title2", "Title1"}, ml.titles(), "The titles are not matching");
			}
		}

		@Test
		void getMediaByTitle() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type LIST
				title-contains Title2
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			new RequestHandler(dummy, s->{}, s->{}).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			MediaList ml = (MediaList) new ObjectInputStream(inputStream).readObject();
			assertEquals(1, ml.ids().length, "The number of ids does not match");
			assertEquals(1, ml.titles().length, "The number of titles does not match");
			assertEquals("id2", ml.ids()[0], "The id is different");
			assertEquals("Title2", ml.titles()[0], "The title is different");
		}
	}

	@Nested
	class INFO {

		MediaInfo mediaInfo = new MediaInfo(
			"id2",
			"Title2",
			1280,
			720,
			2,
			"null3",
			"null4",
			"null1",
			"null2"
		);

		@Test
		void getMediaInfo() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type INFO
				media-id id2
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			new RequestHandler(dummy, s->{}, s->{}).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			MediaInfo info = (MediaInfo) new ObjectInputStream(inputStream).readObject();
			assertEquals(mediaInfo, info);
		}
	}

	@Nested
	class FETCH {

		@Test
		void fetchOnePiece() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type FETCH
				media-id id1
				starting-playback-piece 0
				total-playback-pieces 1
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			new RequestHandler(dummy, s->{}, s->{}).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			FetchedPieces fetchedPieces = (FetchedPieces) new ObjectInputStream(inputStream).readObject();

			assertEquals("id1", fetchedPieces.id(), "The id doesn't match");
			assertEquals(0, fetchedPieces.startingPieceIndex(), "The starting piece doesn't match");
			assertEquals(1, fetchedPieces.video().length, "The number of video pieces don't match");
			assertEquals(1, fetchedPieces.audio().length, "The number of audio pieces don't match");

			assertEquals("title1 video 0", new String(fetchedPieces.video()[0]), "The video content doesn't match");
			assertEquals("title1 audio 0", new String(fetchedPieces.audio()[0]), "The audio content doesn't match");
		}

		@Test
		void fetchSeverPieces() throws IOException, ClassNotFoundException{
			byte[] request = """
				request-type FETCH
				media-id id2
				starting-playback-piece 0
				total-playback-pieces 2
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			new RequestHandler(dummy, s->{}, s->{}).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			FetchedPieces fetchedPieces = (FetchedPieces) new ObjectInputStream(inputStream).readObject();

			assertEquals("id2", fetchedPieces.id(), "The id doesn't match");
			assertEquals(0, fetchedPieces.startingPieceIndex(), "The starting piece doesn't match");
			assertEquals(2, fetchedPieces.video().length, "The number of video pieces doesn't match");
			assertEquals(2, fetchedPieces.audio().length, "The number of audio pieces doesn't match");

			assertEquals("title2 video 0", new String(fetchedPieces.video()[0]), "The video content doesn't match");
			assertEquals("title2 video 1", new String(fetchedPieces.video()[1]), "The video content doesn't match");
			assertEquals("title2 audio 0", new String(fetchedPieces.audio()[0]), "The audio content doesn't match");
			assertEquals("title2 audio 1", new String(fetchedPieces.audio()[1]), "The audio content doesn't match");
		}

		@Test
		void fetchRange() throws IOException, ClassNotFoundException {
			byte[] request = """
				request-type FETCH
				media-id id2
				starting-playback-piece 1
				total-playback-pieces 1
				body-length 0
				
				""".getBytes();
			dummy.write(request);
			new RequestHandler(dummy, s->{}, s->{}).run();

			byte[] response = new byte[10000];
			int responseLen = dummy.read(response);
			int bodyIndex = 1;
			while (++bodyIndex < responseLen && (response[bodyIndex - 2] != '\n' || response[bodyIndex - 1] != '\n'));

			ByteArrayInputStream inputStream = new ByteArrayInputStream(response, bodyIndex, responseLen - bodyIndex);
			FetchedPieces fetchedPieces = (FetchedPieces) new ObjectInputStream(inputStream).readObject();

			assertEquals("id2", fetchedPieces.id(), "The id doesn't match");
			assertEquals(1, fetchedPieces.startingPieceIndex(), "The starting piece doesn't match");
			assertEquals(1, fetchedPieces.video().length, "The number of video pieces doesn't match");
			assertEquals(1, fetchedPieces.audio().length, "The number of auiod pieces doesn't match");

			assertEquals("title2 video 1", new String(fetchedPieces.video()[0]), "The video content doesn't match");
			assertEquals("title2 audio 1", new String(fetchedPieces.audio()[0]), "The audio content doesn't match");
		}
	}
}
