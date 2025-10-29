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

package backend.persistence;

import backend.exceptions.CommonDataAccessException;
import backend.exceptions.CorruptedDataException;
import backend.exceptions.QueryingStrategyFactoryException;
import backend.stubs.QueryingStrategyFactoryStub;
import backend.stubs.SqlAccessStrategyStub;
import backend.stubs.SqlRowStub;
import backend.models.Media;
import backend.models.SqlRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SqlMediaDataAccessTests {

	QueryingStrategyFactoryStub queryingStrategyFactoryStub = new QueryingStrategyFactoryStub();

	SqlAccessStrategyStub sqlAccessStrategyStub = new SqlAccessStrategyStub();

	SqlMediaDataAccess sqlMediaDataAccess = new SqlMediaDataAccess(queryingStrategyFactoryStub, sqlAccessStrategyStub);

	@BeforeEach
	void beforeEach() {
		queryingStrategyFactoryStub.getQueryingStrategyFunction = uri -> {
			assertEquals(URI.create("test_uri"), uri);
			return queryingStrategyFactoryStub.queryingStrategyStub;
		};
	}

	@Nested
	class RetrieveAllMedia {

		@Test
		void failDataAccess() {
			sqlAccessStrategyStub.queryMultipleSqlRowsFunction = columns -> {
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				throw new SQLException();
			};

			assertThrows(
				CommonDataAccessException.class,
				() -> sqlMediaDataAccess.getMedia(),
				"The querying method didn't throw " + CommonDataAccessException.class.getSimpleName()
			);
		}

		@Test
		void failQueryingStrategyInstantiation() {
			sqlAccessStrategyStub.queryMultipleSqlRowsFunction = columns -> {
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				return Stream.of(generateSqlRows(1));
			};
			queryingStrategyFactoryStub.getQueryingStrategyFunction = uri -> {
				assertEquals(URI.create("test_uri"), uri);

				throw new QueryingStrategyFactoryException();
			};

			assertEquals(0, sqlMediaDataAccess.getMedia().length, "An empty stream is expected");
		}

		@Test
		void corruptDataHandlingTest() {
			sqlAccessStrategyStub.queryMultipleSqlRowsFunction = columns -> {
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				SqlRowStub sqlRow = new SqlRowStub();
				for (String column: columns) {
					sqlRow.hashMap.put(column, null);
				}
				return Stream.of(sqlRow);
			};

			assertEquals(0, sqlMediaDataAccess.getMedia().length, "An empty stream is expected");
		}

		@ParameterizedTest
		@ValueSource(ints = {0, 1, 2})
		void retrievalTest(int resultSize) {
			SqlRow[] result = generateSqlRows(resultSize);
			sqlAccessStrategyStub.queryMultipleSqlRowsFunction = columns -> {
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				return Arrays.stream(result);
			};

			Media[] retrievedMedia = sqlMediaDataAccess.getMedia();
			assertEquals(
				resultSize, retrievedMedia.length, "The number of media doesn't match the amount of sql rows"
			);
			for (int i = 0; i < resultSize; i++) {
				SqlRow cRow = result[i];
				Media cMedia = retrievedMedia[i];
				assertNotNull(cMedia, "The result contains null elements");
				assertEquals(
					cRow.getString("id"),
					cMedia.getID().toString(),
					"The media id doesn't match in the media no. " + i
				);
				assertEquals(
					cRow.getString("title"),
					cMedia.getTitle(),
					"The title doesn't match in the media no. " + i
				);
				assertEquals(
					cRow.getInt("duration"),
					cMedia.getDuration(),
					"The duration value doesn't match in the media no. " + i
				);
				assertEquals(
					cRow.getString("media_content_uri"),
					cMedia.getContentURI().toString(),
					"The content URI doesn't match in the media no. " + i
				);
			}
		}
	}

	@Nested
	class RetrieveSingleMedia {

		@Test
		void failDataAccess() {
			UUID mediaId = UUID.randomUUID();
			sqlAccessStrategyStub.querySingleSqlRowFunction = (k, columns) -> {
				assertEquals(mediaId.toString(), k, "The provided media id doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				throw new SQLException();
			};

			assertThrows(
				CommonDataAccessException.class,
				() -> sqlMediaDataAccess.getMedia(mediaId),
				"The querying method didn't throw " + CommonDataAccessException.class.getSimpleName()
			);
		}

		@Test
		void failQueryingStrategyInstantiation() {
			UUID mediaId = UUID.randomUUID();
			sqlAccessStrategyStub.querySingleSqlRowFunction = (key, columns) -> {
				assertEquals(mediaId.toString(), key, "The provided id doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				return generateSqlRows(1)[0];
			};
			queryingStrategyFactoryStub.getQueryingStrategyFunction = uri -> {
				assertEquals(URI.create("test_uri"), uri);
				throw new QueryingStrategyFactoryException();
			};

			assertThrows(
				CorruptedDataException.class,
				() -> sqlMediaDataAccess.getMedia(mediaId),
				"The querying method didn't throw " + CorruptedDataException.class
			);
		}

		@Test
		void corruptDataHandlingTest() {
			UUID mediaId = UUID.randomUUID();
			sqlAccessStrategyStub.querySingleSqlRowFunction = (key, columns) -> {
				assertEquals(mediaId.toString(), key, "The provided id doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				SqlRowStub sqlRowStub = new SqlRowStub();
				for (String column: columns) {
					sqlRowStub.hashMap.put(column, null);
				}
				return sqlRowStub;
			};

			assertThrows(
				CorruptedDataException.class,
				() -> sqlMediaDataAccess.getMedia(mediaId),
				"The querying method didn't throw " + CorruptedDataException.class.getSimpleName()
			);
		}

		@Test
		void mediaNotFoundTest() {
			UUID mediaId = UUID.randomUUID();
			sqlAccessStrategyStub.querySingleSqlRowFunction = (key, columns) -> {
				assertEquals(mediaId.toString(), key, "The provided id doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				return null;
			};

			assertNull(
				sqlMediaDataAccess.getMedia(mediaId),
				"The null value is expected when no media is associated with the id"
			);
		}

		@Test
		void retrievalTest() {
			SqlRow sqlRowStub = generateSqlRows(1)[0];
			String mediaId = sqlRowStub.getString("id");

			sqlAccessStrategyStub.querySingleSqlRowFunction = (key, columns) -> {
				assertEquals(mediaId, key, "The provided media id doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				return sqlRowStub;
			};

			Media retrievedMedia = sqlMediaDataAccess.getMedia(UUID.fromString(mediaId));

			assertNotNull(retrievedMedia, "The retrieval failed");
			assertEquals(mediaId, retrievedMedia.getID().toString(), "The media id doesn't match");
			assertEquals(
				sqlRowStub.getString("title"), retrievedMedia.getTitle(), "The title doesn't match"
			);
			assertEquals(
				sqlRowStub.getInt("duration"),
				retrievedMedia.getDuration(),
				"The duration value doesn't match"
			);
			assertEquals(
				sqlRowStub.getString("media_content_uri"),
				retrievedMedia.getContentURI().toString(),
				"The content URI doesn't match"
			);
		}
	}

	@Nested
	class SearchMedia {

		@Test
		void failDataAccess() {
			String searchQuery = "query";
			sqlAccessStrategyStub.searchItTitleFunction = (query, columns) -> {
				assertEquals(searchQuery, query, "The passed search query doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				throw new SQLException();
			};

			assertThrows(
				CommonDataAccessException.class,
				() -> sqlMediaDataAccess.searchMedia(searchQuery),
				"The querying method didn't throw " + CommonDataAccessException.class
			);
		}

		@Test
		void failQueryingStrategyInstantiation() {
			String searchQuery = "query";
			sqlAccessStrategyStub.searchItTitleFunction = (query, columns) -> {
				assertEquals(searchQuery, query, "The passed search query doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				return Stream.of(generateSqlRows(1));
			};
			queryingStrategyFactoryStub.getQueryingStrategyFunction = uri -> {
				assertEquals(URI.create("test_uri"), uri);
				throw new QueryingStrategyFactoryException();
			};

			assertEquals(
				0,
				sqlMediaDataAccess.searchMedia(searchQuery).length,
				"An empty stream is expected"
			);
		}

		@Test
		void corruptedDataHandlingTest() {
			String searchQuery = "query";
			sqlAccessStrategyStub.searchItTitleFunction = (query, columns) -> {
				assertEquals(searchQuery, query, "The passed search query doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				SqlRowStub sqlRowStub = new SqlRowStub();
				for (String column: columns) {
					sqlRowStub.hashMap.put(column, null);
				}
				return Stream.of(sqlRowStub);
			};

			assertEquals(
				0,
				sqlMediaDataAccess.searchMedia(searchQuery).length,
				"An empty stream is expected"
			);
		}

		@ParameterizedTest
		@ValueSource(ints = {0, 1, 2})
		void retrievalTest(int resultSize) {
			SqlRow[] result = generateSqlRows(resultSize);
			String searchQuery = "title";
			sqlAccessStrategyStub.searchItTitleFunction = (query, columns) -> {
				assertEquals(searchQuery, query, "The passed search query doesn't match");
				String[] expectedColumns = new String[] {"id", "title", "duration", "media_content_uri"};
				Arrays.sort(expectedColumns);
				Arrays.sort(columns);
				assertArrayEquals(expectedColumns, columns, "Unexpected columns' names");

				return Arrays.stream(result);
			};

			Media[] retrievedMedia = sqlMediaDataAccess.searchMedia(searchQuery);

			assertEquals(
				resultSize, retrievedMedia.length, "The number of media doesn't match the amount of sql rows"
			);
			for (int i = 0; i < resultSize; i++) {
				SqlRow cRow = result[i];
				Media cMedia = retrievedMedia[i];
				assertNotNull(cMedia, "The result contains null elements");
				assertEquals(
					cRow.getString("id"),
					cMedia.getID().toString(),
					"The media id doesn't match in the media no. " + i
				);
				assertEquals(
					cRow.getString("title"),
					cMedia.getTitle(),
					"The title doesn't match in the media no. " + i
				);
				assertEquals(
					cRow.getInt("duration"),
					cMedia.getDuration(),
					"The duration value doesn't match in the media no. " + i
				);
				assertEquals(
					cRow.getString("media_content_uri"),
					cMedia.getContentURI().toString(),
					"The content URI doesn't match in the media no. " + i
				);
			}
		}
	}

	private SqlRow[] generateSqlRows(int amount) {
		SqlRow[] result = new SqlRow[amount];
		for (int i = 0; i < amount; i++) {
			SqlRowStub sqlRowStub = new SqlRowStub();
			sqlRowStub.hashMap.put("id", UUID.randomUUID().toString());
			sqlRowStub.hashMap.put("title", "test title no. " + i);
			sqlRowStub.hashMap.put("duration", i + 1);
			sqlRowStub.hashMap.put("media_content_uri", "test_uri");
			result[i] = sqlRowStub;
		}
		return result;
	}
}
