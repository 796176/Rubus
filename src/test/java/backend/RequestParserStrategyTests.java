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

import common.net.request.RubusRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public abstract class RequestParserStrategyTests {

	String requestExample = """
		request-type LIST
		key1 val1
		key2 val2
		key3 val3
		
		""";

	RequestParserStrategy requestParserStrategyImpl;

	abstract RequestParserStrategy getStrategy();

	@BeforeEach
	void beforeEach() {
		requestParserStrategyImpl = getStrategy();
	}

	@Test
	void feedTest() {
		assertDoesNotThrow(() -> requestParserStrategyImpl.feed(requestExample));
	}

	@Nested
	class RequestFed {

		@BeforeEach
		void beforeEach() {
			requestParserStrategyImpl.feed(requestExample);
		}

		@Test
		void extractType() {
			assertDoesNotThrow(
				() -> {
					RubusRequestType type = requestParserStrategyImpl.type();
					assertEquals(RubusRequestType.LIST, type, "The type doesn't match");
				},
				"The type wasn't extracted"
			);
		}

		@Test
		void extractValues() {
			assertEquals("val1", requestParserStrategyImpl.value("key1"));
			assertEquals("val2", requestParserStrategyImpl.value("key2"));
			assertEquals("val3", requestParserStrategyImpl.value("key3"));
		}

		@Test
		void useKeyRequestDoesNotContain() {
			assertThrows(IllegalArgumentException.class, () -> requestParserStrategyImpl.value("rubus"));
		}

		@Nested
		class AnotherRequestFed {

			String requestExample = """
				request-type INFO
				key4 val4
				key5 val5
				
				""";

			@BeforeEach
			void beforeEach() {
				assertDoesNotThrow(() -> requestParserStrategyImpl.feed(requestExample));
			}

			@Test
			void extractNewValues() {
				assertEquals(RubusRequestType.INFO, requestParserStrategyImpl.type());
				assertEquals("val4", requestParserStrategyImpl.value("key4"));
				assertEquals("val5", requestParserStrategyImpl.value("key5"));
			}

			@Test
			void tryExtractOldValues() {
				assertThrows(IllegalArgumentException.class, () -> requestParserStrategyImpl.value("key1"));
				assertThrows(IllegalArgumentException.class, () -> requestParserStrategyImpl.value("key2"));
				assertThrows(IllegalArgumentException.class, () -> requestParserStrategyImpl.value("key3"));
			}
		}
	}
}
