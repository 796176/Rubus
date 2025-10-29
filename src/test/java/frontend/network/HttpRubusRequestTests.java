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

package frontend.network;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class HttpRubusRequestTests {

	final static String host = "example.com";

	final static String altHost = "example.org";

	final static int port = 777;

	final static int altPort = 1000;

	@Nested
	@ParameterizedClass
	@ArgumentsSource(UnparameterizedListRequest.UnparameterizedListRequestArgumentProvider.class)
	class UnparameterizedListRequest {

		static class UnparameterizedListRequestArgumentProvider implements ArgumentsProvider {
			@Override
			public Stream<Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
				return Stream.of(
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(host)
							.port(port)
							.LIST()
							.build(),
						host + ":" + port
					),
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(altHost)
							.port(altPort)
							.LIST()
							.build(),
						altHost + ":" + altPort
					)
				);
			}
		}

		@Parameter(0)
		HttpRubusRequest httpRubusRequest;

		@Parameter(1)
		String expectedShemalessUriWithoutQuery;

		@Test
		void httpUriWithoutQueryValidity() {
			assertTrue(
				httpRubusRequest.getHttpUri().toString().startsWith("http://" + expectedShemalessUriWithoutQuery)
			);
		}

		@Test
		void httpsUriWithoutQueryValidity() {
			assertTrue(
				httpRubusRequest.getHttpsUri().toString().startsWith("https://" + expectedShemalessUriWithoutQuery)
			);
		}

		@Test
		void queryParametersEquality() {
			String[] httpQueryParameters = httpRubusRequest.getHttpUri().getRawQuery().split("&");
			Arrays.sort(httpQueryParameters);
			String[] httpsQueryParameters = httpRubusRequest.getHttpsUri().getRawQuery().split("&");
			Arrays.sort(httpsQueryParameters);

			assertArrayEquals(httpQueryParameters, httpsQueryParameters);
		}

		@Test
		void queryParametersValidity() {
			String[] queryParameters = httpRubusRequest.getHttpUri().getRawQuery().split("&");
			Arrays.sort(queryParameters);
			String[] expectedQueryParameters = new String[] {"request_type=LIST", "search_query="};

			assertArrayEquals(expectedQueryParameters, queryParameters);
		}
	}

	@Nested
	@ParameterizedClass
	@ArgumentsSource(ListRequest.ListRequestArgumentProvider.class)
	class ListRequest {

		static class ListRequestArgumentProvider implements ArgumentsProvider {
			@Override
			public Stream<Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
				return Stream.of(
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(host)
							.port(port)
							.LIST("")
							.build(),
						new String[] {"request_type=LIST", "search_query="},
						host + ":" + port
					),
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(altHost)
							.port(altPort)
							.LIST("qwerty")
							.build(),
						new String[] {"request_type=LIST", "search_query=qwerty"},
						altHost + ":" + altPort
					),
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(host)
							.port(port)
							.LIST("&")
							.build(),
						new String[] {"request_type=LIST", "search_query=%26"},
						host + ":" + port
					)
				);
			}
		}

		@Parameter(0)
		HttpRubusRequest httpRubusRequest;

		@Parameter(1)
		String[] expectedQueryParameters;

		@Parameter(2)
		String expectedSchemalessUriWithoutQuery;

		@Test
		void httpUriWithoutQueryValidation() {
			assertTrue(
				httpRubusRequest.getHttpUri().toString().startsWith("http://" + expectedSchemalessUriWithoutQuery)
			);
		}

		@Test
		void httpsUriWithoutQueryValidation() {
			assertTrue(
				httpRubusRequest.getHttpsUri().toString().startsWith("https://" + expectedSchemalessUriWithoutQuery)
			);
		}

		@Test
		void queryParametersEquality() {
			String[] httpQueryParameters = httpRubusRequest.getHttpUri().getRawQuery().split("&");
			Arrays.sort(httpQueryParameters);
			String[] httpsQueryParameters = httpRubusRequest.getHttpsUri().getRawQuery().split("&");
			Arrays.sort(httpsQueryParameters);

			assertArrayEquals(httpQueryParameters, httpsQueryParameters);
		}

		@Test
		void uriParametersValidity() {
			String[] queryParameters = httpRubusRequest.getHttpUri().getRawQuery().split("&");
			Arrays.sort(queryParameters);

			assertArrayEquals(expectedQueryParameters, queryParameters);
		}
	}

	@Nested
	@ParameterizedClass
	@ArgumentsSource(InfoRequest.InfoRequestArgumentProvider.class)
	class InfoRequest {

		static class InfoRequestArgumentProvider implements ArgumentsProvider {
			@Override
			public Stream<Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
				return Stream.of(
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(host)
							.port(port)
							.INFO("test_id")
							.build(),
						new String[] {"media_id=test_id", "request_type=INFO"},
						host + ":" + port
					),
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(altHost)
							.port(altPort)
							.INFO("a&b")
							.build(),
						new String[] {"media_id=a%26b", "request_type=INFO"},
						altHost + ":" + altPort
					)
				);
			}
		}

		@Parameter(0)
		HttpRubusRequest httpRubusRequest;

		@Parameter(1)
		String[] expectedQueryParameters;

		@Parameter(2)
		String expectedShemalessUriWithoutQuery;

		@Test
		void httpUriWithoutQueryValidation() {
			assertTrue(
				httpRubusRequest.getHttpUri().toString().startsWith("http://" + expectedShemalessUriWithoutQuery)
			);
		}

		@Test
		void httpsUriWithoutQueryValidation() {
			assertTrue(
				httpRubusRequest.getHttpsUri().toString().startsWith("https://" + expectedShemalessUriWithoutQuery)
			);
		}

		@Test
		void queryParametersEquality() {
			String[] httpQueryParameters = httpRubusRequest.getHttpUri().getRawQuery().split("&");
			Arrays.sort(httpQueryParameters);
			String[] httpsQueryParameters = httpRubusRequest.getHttpsUri().getRawQuery().split("&");
			Arrays.sort(httpsQueryParameters);

			assertArrayEquals(httpQueryParameters, httpsQueryParameters);
		}

		@Test
		void queryParametersValidity() {
			String[] queryParameters = httpRubusRequest.getHttpUri().getRawQuery().split("&");
			Arrays.sort(queryParameters);

			assertArrayEquals(expectedQueryParameters, queryParameters);
		}
	}

	@Nested
	@ParameterizedClass
	@ArgumentsSource(FetchRequest.FetchRequestArgumentProvider.class)
	class FetchRequest {

		static class FetchRequestArgumentProvider implements ArgumentsProvider {
			@Override
			public Stream<Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
				return Stream.of(
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(host)
							.port(port)
							.FETCH("test_id", 0, 1)
							.build(),
						new String[] {"clip_amount=1", "clip_offset=0", "media_id=test_id", "request_type=FETCH"},
						host + ":" + port
					),
					Arguments.of(
						new HttpRubusRequest.Builder()
							.host(altHost)
							.port(altPort)
							.FETCH("a&b", 5, 10)
							.build(),
						new String[] {"clip_amount=10", "clip_offset=5", "media_id=a%26b", "request_type=FETCH"},
						altHost + ":" + altPort
					)
				);
			}
		}

		@Parameter(0)
		HttpRubusRequest httpRubusRequest;

		@Parameter(1)
		String[] expectedQueryParameters;

		@Parameter(2)
		String expectedShemalessUriWithoutQuery;

		@Test
		void httpUriWithoutQueryValidation() {
			assertTrue(
				httpRubusRequest.getHttpUri().toString().startsWith("http://" + expectedShemalessUriWithoutQuery)
			);
		}

		@Test
		void httpsUriWithoutQueryValidation() {
			assertTrue(
				httpRubusRequest.getHttpsUri().toString().startsWith("https://" + expectedShemalessUriWithoutQuery)
			);
		}

		@Test
		void queryParametersEquality() {
			String[] httpQueryParameters = httpRubusRequest.getHttpUri().getRawQuery().split("&");
			Arrays.sort(httpQueryParameters);
			String[] httpsQueryParameters = httpRubusRequest.getHttpsUri().getRawQuery().split("&");
			Arrays.sort(httpsQueryParameters);

			assertArrayEquals(httpQueryParameters, httpsQueryParameters);
		}

		@Test
		void queryParametersValidity() {
			String[] queryParameters = httpRubusRequest.getHttpUri().getRawQuery().split("&");
			Arrays.sort(queryParameters);

			assertArrayEquals(expectedQueryParameters, queryParameters);
		}
	}

	@Test
	void buildOmittingQueryParameters() {
		assertThrows(
			IllegalStateException.class,
			() -> {
				HttpRubusRequest.Builder builder = new HttpRubusRequest.Builder().host(host).port(port);
				builder.build();
			}
		);
	}

	@Test
	void buildOmittingPort() {
		assertThrows(
			IllegalStateException.class,
			() -> {
				HttpRubusRequest.Builder builder = new HttpRubusRequest.Builder().host(host).LIST();
				builder.build();
			}
		);
	}

	@Test
	void buildOmittingHost() {
		assertThrows(
			IllegalStateException.class,
			() -> {
				RubusRequest.Builder builder = new HttpRubusRequest.Builder().port(port).LIST();
				builder.build();
			}
		);
	}

	@Nested
	class FetchRequestInvalidRangeParameterization {
		HttpRubusRequest.Builder httpRubusRequestBuilder = new HttpRubusRequest.Builder();

		@Test
		void negativeOffsetValue() {
			assertThrows(
				IllegalArgumentException.class,
				() -> httpRubusRequestBuilder.FETCH("id", -1, 1)
			);
		}

		@Test
		void negativeAmountValue() {
			assertThrows(
				IllegalArgumentException.class,
				() -> httpRubusRequestBuilder.FETCH("id", 0, -1)
			);
		}

		@Test
		void zeroAmountValue() {
			assertThrows(
				IllegalArgumentException.class,
				() -> httpRubusRequestBuilder.FETCH("id", 0, 0)
			);
		}
	}
}
