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

package backend.interactors;

import backend.authorization.ActionType;
import backend.exceptions.CommonDataAccessException;
import backend.stubs.MediaDataAccessStub;
import backend.stubs.ViewerAuthorizerStub;
import backend.stubs.ViewerStub;
import backend.stubs.MediaStub;
import backend.exceptions.AuthorizationException;
import backend.models.Media;
import backend.models.Viewer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultMediaProviderTests {

	ViewerAuthorizerStub viewerAuthorizerStub = new ViewerAuthorizerStub();

	MediaDataAccessStub mediaDataAccessStub = new MediaDataAccessStub();

	Viewer viewer = ViewerStub.getRegularViewer();

	DefaultMediaProvider defaultMediaProvider = new DefaultMediaProvider(viewerAuthorizerStub, mediaDataAccessStub);

	@BeforeEach
	void beforeEach() {
		viewerAuthorizerStub.validateFunction = (v, type) -> {
			assertSame(viewer, v, "The viewer is a different object");
			assertEquals(ActionType.READ, type, "The action type doesn't match");
			return true;
		};
	}

	@Nested
	class RetrieveAllMedia {

		@Test
		void failAuthorization() {
			viewerAuthorizerStub.validateFunction = (v, type) -> false;

			assertThrows(
				AuthorizationException.class,
				() -> defaultMediaProvider.getMedia(viewer),
				"The querying method didn't throw " + AuthorizationException.class.getSimpleName()
			);
		}

		@Test
		void failDataAccess() {
			mediaDataAccessStub.getMultipleMediaSupplier = () -> { throw new CommonDataAccessException(); };

			assertThrows(
				CommonDataAccessException.class,
				() -> defaultMediaProvider.getMedia(viewer),
				"The querying method didn't throw " + CommonDataAccessException.class.getSimpleName()
			);
		}

		@ParameterizedTest
		@ValueSource(ints = {0, 1, 2})
		void retrievalTest(int resultSize) {
			Media[] generatedMedia = new Media[resultSize];
			Arrays.setAll(generatedMedia, i -> new MediaStub());
			mediaDataAccessStub.getMultipleMediaSupplier = () -> generatedMedia;

			Media[] retrievedMedia = defaultMediaProvider.getMedia(viewer);

			assertSame(generatedMedia, retrievedMedia, "The retrieved array is a different array");
		}
	}

	@Nested
	class RetrieveSingleMedia {

		@Test
		void failAuthorization() {
			viewerAuthorizerStub.validateFunction = (v, type) -> {
				assertSame(viewer, v, "The viewer is a different object");
				assertEquals(ActionType.READ, type, "The action type doesn't match");
				return false;
			};

			assertThrows(
				AuthorizationException.class,
				() -> defaultMediaProvider.getMedia(viewer, UUID.randomUUID()),
				"The querying method didn't throw " + AuthorizationException.class.getSimpleName()
			);
		}

		@Test
		void failDataAccess() {
			UUID mediaId = UUID.randomUUID();
			mediaDataAccessStub.getSingleMediaFunction = id -> {
				assertEquals(mediaId, id, "The passed media id is different");
				throw new CommonDataAccessException();
			};

			assertThrows(
				CommonDataAccessException.class,
				() -> defaultMediaProvider.getMedia(viewer, mediaId),
				"The querying method didn't throw " + CommonDataAccessException.class.getSimpleName()
			);
		}

		@Test
		void retrieveNonExistentMediaTest() {
			UUID mediaId = UUID.randomUUID();
			mediaDataAccessStub.getSingleMediaFunction = id -> {
				assertEquals(mediaId, id, "The passed media id is different");
				return null;
			};

			assertNull(
				defaultMediaProvider.getMedia(viewer, mediaId),
				"Null should be returned when no media is associated with the id"
			);
		}

		@Test
		void retrievalTest() {
			MediaStub mediaStub = new MediaStub();
			mediaDataAccessStub.getSingleMediaFunction = id -> {
				assertEquals(mediaStub.getID(), id, "The passed media id is different");
				return mediaStub;
			};

			Media retrievedMedia = defaultMediaProvider.getMedia(viewer, mediaStub.getID());
			assertSame(mediaStub, retrievedMedia, "The retrieved media is a different object");
		}
	}

	@Nested
	class SearchMedia {

		@Test
		void failAuthorization() {
			viewerAuthorizerStub.validateFunction = (v, type) -> false;

			assertThrows(
				AuthorizationException.class,
				() -> defaultMediaProvider.searchMedia(viewer, ""),
				"The querying method didn't throw " + AuthorizationException.class.getSimpleName()
			);
		}

		@Test
		void failDataAccess() {
			String searchQuery = "query";
			mediaDataAccessStub.searchMediaFunction = query -> {
				assertEquals(searchQuery, query, "The passed search query is different");
				throw new CommonDataAccessException();
			};

			assertThrows(
				CommonDataAccessException.class,
				() -> defaultMediaProvider.searchMedia(viewer, searchQuery),
				"The querying method didn't throw " + CommonDataAccessException.class.getSimpleName()
			);
		}

		@ParameterizedTest
		@ValueSource(ints = {0, 1, 2})
		void searchTest(int resultSize) {
			Media[] generatedMedia = new Media[resultSize];
			Arrays.setAll(generatedMedia, i -> new MediaStub());
			String searchQuery = "" + resultSize;
			mediaDataAccessStub.searchMediaFunction = (query) -> {
				assertEquals(searchQuery, query, "The passed search query is different");
				return generatedMedia;
			};

			Media[] retreivedMedia = defaultMediaProvider.searchMedia(viewer, searchQuery);
			assertSame(generatedMedia, retreivedMedia, "The retrieved array is a different array");
		}
	}
}
