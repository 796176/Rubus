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

package common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTests {

	Path configPath;

	Config config;

	@BeforeEach
	void beforeEach() throws IOException {
		configPath = Files.createTempFile(null, null);
		config = new Config(configPath);
	}

	@AfterEach
	void afterEach() throws IOException {
		Files.delete(configPath);
	}

	@Test
	void extractNonExistentKey() {
		assertNull(config.get("key"));
	}

	@Test
	void safeEmptyConfig() throws IOException {
		config.save();
		String configContent = Files.readString(configPath);
		assertTrue(configContent.isBlank());
	}

	@Nested
	class AddKey {

		String key = "testKey";

		String val = "testValue";

		@BeforeEach
		void beforeEach() {
			config.set(key, val);
		}

		@Test
		void extractKey() {
			assertEquals(val, config.get(key));
		}

		@Test
		void rewriteKey() {
			String val = "newValue";
			config.set(key, val);
			assertEquals(val, config.get(key));
		}

		@Test
		void safeConfig() throws IOException {
			config.save();
			String configContent = Files.readString(configPath);
			assertTrue(configContent.contains(key + " " + val));
		}

		@Nested
		class RemoveKey {

			@BeforeEach
			void beforeEach() {
				config.remove(key);
			}

			@Test
			void extractRemovedKey() {
				assertNull(config.get(key));
			}
		}

		@Nested
		class AddSecondKey {

			String key2 = "testKey2";

			String val2 = "testValue2";

			@BeforeEach
			void beforeEach() {
				config.set(key2, val2);
			}

			@Test
			void extractSecondKey() {
				assertEquals(val2, config.get(key2));
			}

			@Test
			void safeConfig() throws IOException {
				config.save();
				String configContent = Files.readString(configPath);
				assertTrue(configContent.contains(key + " " + val), "The first key is absent");
				assertTrue(configContent.contains(key2 + " " + val2), "The second key is absent");
			}
		}
	}
}
