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

package frontend.network;

import jakarta.annotation.Nonnull;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * A concrete implementation of {@link RubusClient} using the HTTP application layer protocol. By default, this class
 * attempts to make a request using the https protocol; if it fails it falls back to the http protocol.
 */
public class HttpRubusClient implements RubusClient {

	private final String remoteHost;

	private final int remotePort;

	private final HttpClient client;

	private boolean secureConnectionRequired = false;

	private boolean secureConnectionEnabled = true;

	/**
	 * Constructs an instance of this class.
	 * @param remoteHost the name of the remote host
	 * @param remotePort the port of the remote host
	 */
	public HttpRubusClient(@Nonnull String remoteHost, int remotePort) {
		assert remotePort <= 65535 && remotePort >= 0;

		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		client = HttpClient.newBuilder().connectTimeout(Duration.of(10, ChronoUnit.SECONDS)).build();
	}

	@Override
	public RubusResponse send(@Nonnull RubusRequest rubusRequest, long timeout) throws InterruptedException, IOException {
		assert timeout >= 0;

		if (rubusRequest instanceof HttpRubusRequest httpRubusRequest) {
			HttpRequest.Builder requestBuilder = HttpRequest
				.newBuilder()
				.GET()
				.timeout(Duration.of(timeout, ChronoUnit.MILLIS));
			if (secureConnectionEnabled) {
				try {
					HttpRequest request = requestBuilder.uri(httpRubusRequest.getHttpsUri()).build();
					HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
					return new HttpRubusResponse(response.body(), response.statusCode());
				} catch (SSLException e) {
					if (secureConnectionRequired) throw e;
				}
			}

			HttpRequest request = requestBuilder.uri(httpRubusRequest.getHttpUri()).build();
			HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
			return new HttpRubusResponse(response.body(), response.statusCode());
		}

		throw new IllegalArgumentException("Illegal RubusRequest type");
	}

	@Override
	public RubusRequest.Builder getRequestBuilder() {
		return new HttpRubusRequest.Builder().host(remoteHost).port(remotePort);
	}

	@Override
	public void close() throws IOException {
		if (client != null) {
			client.shutdownNow();
		}
	}

	/**
	 * Returns true if secure connection is enabled, false otherwise.
	 * @return true if secure connection is enabled, false otherwise
	 */
	public boolean isSecureConnectionEnabled() {
		return secureConnectionEnabled;
	}

	/**
	 * Configures if this client should attempt to make a request using the https protocol.
	 * @param isEnabled whether secure connection should be enabled
	 */
	public void setSecureConnectionEnabled(boolean isEnabled) {
		secureConnectionEnabled = isEnabled;
	}

	/**
	 * Returns true if secure connection is required, false otherwise.
	 * @return true if secure connection is required, false otherwise
	 */
	public boolean isSecureConnectionRequired() {
		return secureConnectionRequired;
	}

	/**
	 * Configures if this client should fall back to using the http protocol after making a request using the https
	 * protocol fails.
	 * @param isRequired whether secure connection is required
	 */
	public void setSecureConnectionRequired(boolean isRequired) {
		secureConnectionRequired = isRequired;
	}
}
