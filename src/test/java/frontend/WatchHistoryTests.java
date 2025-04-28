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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class WatchHistoryTests {

	WatchHistory watchHistory;

	@Nested
	class InstantiatingWithEmptyFile {

		@BeforeEach
		void beforeEach() throws IOException {
			Path watchHistoryPath = Files.createTempFile(null, null);
			watchHistory = new WatchHistory(watchHistoryPath);
		}

		@AfterEach
		void afterEach() throws IOException {
			watchHistory.close();
		}

		@Test
		void tryRetrieveProgressUsingNotSetId() {
			assertEquals(-1, watchHistory.getProgress("ab"));
		}

		@Nested
		class ProgressAdded {

			@BeforeEach
			void beforeEach() throws IOException {
				watchHistory.setProgress("ab", 42);
			}

			@Test
			void tryRetrieveProgressUsingNotSetId() {
				assertEquals(-1, watchHistory.getProgress("cd"));
			}

			@Test
			void retrieveProgress() {
				assertEquals(42, watchHistory.getProgress("ab"));
			}

			@Nested
			class Purging {

				@BeforeEach
				void beforeEach() throws IOException {
					watchHistory.purge();
				}

				@Test
				void retrievePurgedProgress() {
					assertEquals(-1, watchHistory.getProgress("ab"));
				}
			}
		}
	}

	@Nested
	class InstantiatingWithNotEmptyFile {

		Path watchHistoryPath;

		@BeforeEach
		void beforeEach() throws IOException {
			watchHistoryPath = Files.createTempFile(null, null);
			Files.write(watchHistoryPath, "cd 0000000f\n".getBytes());
			watchHistory = new WatchHistory(watchHistoryPath);
		}

		@AfterEach
		void afterEach() throws IOException {
			watchHistory.close();
		}

		@Test
		void retrieveProgress() {
			assertEquals(15, watchHistory.getProgress("cd"));
		}

		@Test
		void tryRetrieveProgressUsingNotSetId() {
			assertEquals(-1, watchHistory.getProgress("ab"));
		}

		@Nested
		class PurgingAndReInstantiating {

			@BeforeEach
			void beforeEach() throws IOException{
				watchHistory.purge();
				watchHistory.close();
				watchHistory = new WatchHistory(watchHistoryPath);
			}

			@Test
			void retrievePurgedProgress() {
				assertEquals(-1, watchHistory.getProgress("cd"));
			}
		}
	}
}
