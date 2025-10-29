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

import jakarta.annotation.Nonnull;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * A concrete implementation of {@link RubusRequest} that represents rubus request messages as URIs.
 */
public class HttpRubusRequest implements RubusRequest {

	public static class Builder implements RubusRequest.Builder {

		private String remoteHost;

		private int remotePort = -1;

		private Map<String, String> uriParameters;

		public Builder() { }

		@Override
		public HttpRubusRequest.Builder host(@Nonnull String host) {
			remoteHost = host;
			return this;
		}

		@Override
		public HttpRubusRequest.Builder port(int port) {
			if (port > 65535 || port < 0) {
				throw new IllegalArgumentException("The port value is outside the permitted range");
			}
			remotePort = port;
			return this;
		}

		@Override
		public HttpRubusRequest.Builder LIST() {
			uriParameters = Map.of("request_type", "LIST", "search_query", "");
			return this;
		}

		@Override
		public HttpRubusRequest.Builder LIST(@Nonnull String searchQuery) {
			uriParameters = Map.of("request_type", "LIST", "search_query", searchQuery);
			return this;
		}

		@Override
		public HttpRubusRequest.Builder INFO(@Nonnull String mediaId) {
			uriParameters = Map.of("request_type", "INFO", "media_id", mediaId);
			return this;
		}

		@Override
		public HttpRubusRequest.Builder FETCH(@Nonnull String mediaId, int offset, int amount) {
			if (offset < 0) throw new IllegalArgumentException("The offset value can't be negative");
			if (amount <= 0) throw new IllegalArgumentException("The amount value must be positive");

			uriParameters = Map.of(
				"request_type", "FETCH",
				"media_id", mediaId,
				"clip_offset", "" + offset,
				"clip_amount", "" + amount
			);
			return this;
		}

		@Override
		public HttpRubusRequest build() {
			if (uriParameters == null) throw new IllegalStateException("The URI query parameters aren't specified");
			if (remotePort == -1) throw new IllegalStateException("The port isn't assigned");
			if (remoteHost == null) throw new IllegalStateException("The remote host isn't assigned");

			URI httpUri = URI.create("http://" + remoteHost + ":" + remotePort);
			UriComponentsBuilder httpLink = UriComponentsBuilder.fromUri(httpUri).encode();
			for (Map.Entry<String, String> entry: uriParameters.entrySet()) {
				httpLink.queryParam(entry.getKey(), entry.getValue());
			}

			URI httpsUri = URI.create("https://" + remoteHost + ":" + remotePort);
			UriComponentsBuilder httpsLink = UriComponentsBuilder.fromUri(httpsUri).encode();
			for (Map.Entry<String, String> entry: uriParameters.entrySet()) {
				httpsLink.queryParam(entry.getKey(), entry.getValue());
			}
			return new HttpRubusRequest(httpLink.build().toUri(), httpsLink.build().toUri());
		}
	}

	private final URI httpUri;

	private final URI httpsUri;

	private HttpRubusRequest(@Nonnull URI httpLink, @Nonnull URI httpsLink) {
		httpUri = httpLink;
		httpsUri = httpsLink;
	}

	/**
	 * Returns the URI with the HTTP schema.
	 * @return the URI with the HTTP schema
	 */
	public URI getHttpUri() {
		return httpUri;
	}

	/**
	 * Returns the URI with the HTTPS schema.
	 * @return the URI with the HTTPS schema
	 */
	public URI getHttpsUri() {
		return httpsUri;
	}
}
