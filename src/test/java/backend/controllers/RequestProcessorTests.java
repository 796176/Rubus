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

package backend.controllers;

import backend.exceptions.CommonDataAccessException;
import backend.stubs.*;
import backend.stubs.SeekableByteChannelStub;
import backend.exceptions.AuthenticationException;
import backend.exceptions.InvalidParameterException;
import backend.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RequestProcessorTests {

	RequestOriginator requestOriginator = new RequestOriginatorStub("abcd");

	Viewer viewerStub = ViewerStub.getRegularViewer();

	AuthenticatorStub authenticatorStub = new AuthenticatorStub();

	MediaProviderStub mediaProviderStub = new MediaProviderStub();

	RequestProcessor requestProcessor = new RequestProcessor(mediaProviderStub, authenticatorStub);

	@BeforeEach
	void beforeEach() {
		authenticatorStub.authenticateFunction = ro -> {
			assertSame(requestOriginator, ro, "The passed request originator is a different object");
			return viewerStub;
		};
	}

	@Nested
	class SearchMedia {

		@Test
		void failAuthentication() {
			authenticatorStub.authenticateFunction = ro -> {
				assertSame(requestOriginator, ro, "The passed request originator is a different object");
				throw new AuthenticationException();
			};

			assertThrows(
				AuthenticationException.class,
				() -> requestProcessor.listRequest("", requestOriginator),
				"The querying method didn't throw " + AuthenticationException.class.getSimpleName()
			);
		}

		@Test
		void failDataAccess() {
			String searchQuery = "query";
			mediaProviderStub.searchMedia = (v, query) -> {
				assertSame(viewerStub, v, "The passed viewer object is different");
				assertEquals(searchQuery, query, "The passed query id different");
				throw new CommonDataAccessException();
			};

			assertThrows(
				CommonDataAccessException.class,
				() -> requestProcessor.listRequest(searchQuery, requestOriginator),
				"The querying method didn't throw " + CommonDataAccessException.class.getSimpleName()
			);
		}

		@ParameterizedTest
		@ValueSource(ints = {0, 1, 2})
		void searchTest(int resultSize) {
			Media[] generatedMedia = new Media[resultSize];
			Arrays.setAll(generatedMedia, i -> new MediaStub());
			String searchQuery = "" + resultSize;
			mediaProviderStub.searchMedia = (v, query) -> {
				assertSame(viewerStub, v, "The passed viewer object is different");
				assertEquals(searchQuery, query, "The passed query id different");
				return generatedMedia;
			};

			MediaList mediaList = requestProcessor.listRequest(searchQuery, requestOriginator);

			assertNotNull(mediaList, "The retrieved MediaList instance is null");
			assertEquals(
				generatedMedia.length,
				mediaList.media().size(),
				"The size of the underlying map of the retrieved MediaList instance doesn't match"
			);
			for (Media m: generatedMedia) {
				assertTrue(
					mediaList.media().containsKey(m.getID()),
					"The media id " + m.getID() + " is absent"
				);
				assertEquals(
					m.getTitle(),
					mediaList.media().get(m.getID()),
					"The title of the media with id " + m.getID() + " doesn't match"
				);
			}
		}
	}

	@Nested
	class QueryMediaInfo {

		@Test
		void failAuthentication() {
			authenticatorStub.authenticateFunction = ro -> {
				assertSame(requestOriginator, ro, "The passed request originator is a different object");
				throw new AuthenticationException();
			};

			assertThrows(
				AuthenticationException.class,
				() -> requestProcessor.infoRequest(UUID.randomUUID(), requestOriginator),
				"The querying method didn't throw " + AuthenticationException.class.getSimpleName()
			);
		}

		@Test
		void failDataAccess() {
			UUID mediaId = UUID.randomUUID();
			mediaProviderStub.getSingleMediaStrategy = (viewer, id) -> {
				assertSame(viewerStub, viewer, "The passed viewer is a different object");
				assertEquals(mediaId, id, "The passed media id is different");
				throw new CommonDataAccessException();
			};

			assertThrows(
				CommonDataAccessException.class,
				() -> requestProcessor.infoRequest(mediaId, requestOriginator),
				"The querying method didn't throw " + CommonDataAccessException.class
			);
		}

		@Test
		void queryExistingMedia() {
			MediaStub mediaStub = new MediaStub();
			mediaProviderStub.getSingleMediaStrategy = (viewer, id) -> {
				assertSame(viewerStub, viewer, "The passed viewer is a different object");
				assertEquals(mediaStub.getID(), id, "The passed media id is different");
				return mediaStub;
			};

			MediaInfo mediaInfo = requestProcessor.infoRequest(mediaStub.getID(), requestOriginator);
			assertNotNull(mediaInfo, "The retrieved MediaInfo instance is null");
			assertEquals(mediaStub.getID(), mediaInfo.id(), "The media id doesn't match");
			assertEquals(mediaStub.getTitle(), mediaInfo.title(), "The title doesn't match");
			assertEquals(mediaStub.getDuration(), mediaInfo.duration(), "The duration value doesn't match");
		}

		@Test
		void queryNonExistentMedia() {
			UUID mediaId = UUID.randomUUID();
			mediaProviderStub.getSingleMediaStrategy = (viewer, id) -> {
				assertSame(viewerStub, viewer, "The passed viewer is a different object");
				assertEquals(mediaId, id, "The passed media id is a different");
				return null;
			};
			assertThrows(
				InvalidParameterException.class,
				() -> requestProcessor.infoRequest(mediaId, requestOriginator),
				"The querying method didn't throw " + InvalidParameterException.class.getSimpleName()
			);
		}
	}

	@Nested
	class QueryMediaContent {

		MediaStub mediaStub = new MediaStub();

		@BeforeEach
		void beforeEach() {
			mediaStub.duration = 2;
			mediaProviderStub.getSingleMediaStrategy = (viewer, id) -> {
				assertSame(viewerStub, viewer, "The passed viewer is a different object");
				assertEquals(mediaStub.getID(), id, "The passed media id is different");
				return mediaStub;
			};
		}

		@Test
		void failAuthentication() {
			authenticatorStub.authenticateFunction = ro -> {
				assertSame(requestOriginator, ro, "The passed request originator is a different object");
				throw new AuthenticationException();
			};

			assertThrows(
				AuthenticationException.class,
				() -> requestProcessor.fetchRequest(mediaStub.getID(), 0, 1, requestOriginator),
				"The querying method didn't throw " + AuthenticationException.class.getSimpleName()
			);
		}

		@Test
		void failDataAccess() {
			mediaProviderStub.getSingleMediaStrategy = (viewer, id) -> {
				assertSame(viewerStub, viewer, "The passed viewer is a different object");
				assertEquals(mediaStub.getID(), id, "The passed media id is different");
				throw new CommonDataAccessException();
			};

			assertThrows(
				CommonDataAccessException.class,
				() -> requestProcessor.fetchRequest(mediaStub.getID(), 0, 1, requestOriginator),
				"The querying method didn't throw " + CommonDataAccessException.class.getSimpleName()
			);
		}

		@Test
		void passZeroAmountValue() {
			int offset = 0;
			int amount = 0;
			assertThrows(
				InvalidParameterException.class,
				() -> requestProcessor.fetchRequest(mediaStub.getID(), offset, amount, requestOriginator),
				"The querying method didn't throw " + InvalidParameterException.class.getSimpleName()
			);
		}

		@Test
		void passNegativeOffsetValue() {
			int offset = -1;
			int amount = 0;
			assertThrows(
				InvalidParameterException.class,
				() -> requestProcessor.fetchRequest(mediaStub.getID(), offset, amount, requestOriginator),
				"The querying method didn't throw " + InvalidParameterException.class.getSimpleName()
			);
		}

		@Test
		void passNegativeAmountValue() {
			int offset = 0;
			int amount = -1;
			assertThrows(
				InvalidParameterException.class,
				() -> requestProcessor.fetchRequest(mediaStub.getID(), offset, amount, requestOriginator),
				"The querying method didn't throw " + InvalidParameterException.class.getSimpleName()
			);
		}

		@Test
		void passLargeOffsetValue() {
			int offset = Integer.MAX_VALUE;
			int amount = 1;
			assertThrows(
				InvalidParameterException.class,
				() -> requestProcessor.fetchRequest(mediaStub.getID(), offset, amount, requestOriginator),
				"The querying method didn't throw " + InvalidParameterException.class.getSimpleName()
			);
		}

		@Test
		void passLargeAmountValue() {
			int offset = 0;
			int amount = Integer.MAX_VALUE;
			assertThrows(
				InvalidParameterException.class,
				() -> requestProcessor.fetchRequest(mediaStub.getID(), offset, amount, requestOriginator),
				"The querying method didn't throw " + InvalidParameterException.class.getSimpleName()
			);
		}

		@Test
		void queryNonExistentMedia() {
			UUID mediaId = UUID.randomUUID();
			mediaProviderStub.getSingleMediaStrategy = (viewer, id) -> {
				assertSame(viewerStub, viewer, "The passed viewer is a different object");
				assertEquals(mediaId, id, "The passed media id is different");
				return null;
			};

			assertThrows(
				InvalidParameterException.class,
				() -> requestProcessor.fetchRequest(mediaId, 0, 1, requestOriginator),
				"The querying method didn't throw " + InvalidParameterException.class.getSimpleName()
			);
		}

		public static Stream<Arguments> retrievalTestArgumentProvider() {
			return Stream.of(
				Arguments.of(0, 1),
				Arguments.of(0, 2),
				Arguments.of(1, 1)
			);
		}

		@ParameterizedTest
		@MethodSource("retrievalTestArgumentProvider")
		void retrievalTest(int offset, int amount) {
			SeekableByteChannel[] videoClips = new SeekableByteChannelStub[amount];
			Arrays.setAll(videoClips, i -> new SeekableByteChannelStub(new byte[0]));
			mediaStub.retrieveVideoStrategy = (o, a) -> {
				assertEquals(offset, o, "The passed offset value is different");
				assertEquals(amount, a, "The passed amount value is different");
				return videoClips;
			};
			SeekableByteChannel[] audioClips = new SeekableByteChannelStub[amount];
			Arrays.setAll(audioClips, i -> new SeekableByteChannelStub(new byte[0]));
			mediaStub.retrieveAudioStrategy = (o, a) -> {
				assertEquals(offset, o, "The passed offset value is different");
				assertEquals(amount, a, "The passed amount value is different");
				return audioClips;
			};

			MediaFetch mediaFetch = requestProcessor.fetchRequest(mediaStub.getID(), offset, amount, requestOriginator);

			assertEquals(mediaStub.getID(), mediaFetch.id(), "The media id doesn't match");
			assertEquals(offset, mediaFetch.offset(), "The offset value doesn't match");
			assertEquals(amount, mediaFetch.video().length, "The size of the array of video clips doesn't match");
			assertSame(videoClips, mediaFetch.video(), "The array of video clips is a different array");
			assertEquals(amount, mediaFetch.audio().length, "The size of the array of audio clips doesn't match");
			assertSame(audioClips, mediaFetch.audio(), "The array of audio clips is a different array");
		}
	}
}
