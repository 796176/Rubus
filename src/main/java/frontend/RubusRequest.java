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

import common.net.request.RubusRequestType;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class RubusRequest {
	public static class Builder {
		private Builder() {}

		private RubusRequestType requestType;

		private byte[] request = null;

		private String[] params = null;

		public Builder LIST() {
			requestType = RubusRequestType.LIST;
			return this;
		}

		public Builder INFO() {
			requestType = RubusRequestType.INFO;
			return this;
		}

		public Builder FETCH() {
			requestType = RubusRequestType.FETCH;
			return this;
		}


		public Builder params(String... params) {
			this.params = params;
			return this;
		}

		public RubusRequest build() throws IllegalStateException {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("request-type ").append(requestType).append('\n');
			switch (requestType) {
				case LIST -> {
					if (params == null || params.length == 0) {
						stringBuilder.append("title-contains .\n");
					} else {
						stringBuilder.append("title-contains ").append(params[0]).append('\n');
					}
				}
				case INFO -> {
					if (params != null && params.length > 0) {
						stringBuilder.append("media-id ").append(params[0]).append('\n');
					} else {
						throw new IllegalStateException("ID not specified");
					}
				}
				case FETCH -> {
					if (params != null && params.length > 1) {
						try {
							String stringVal1 = Arrays
								.stream(params)
								.filter(s -> s.toLowerCase().matches(".*id|identifyer.* .*"))
								.findFirst()
								.get();
							String stringVal2 = Arrays
								.stream(params)
								.filter(s -> s.toLowerCase().matches(".*start|begin|from.* \\d+"))
								.findFirst()
								.get();
							String stringVal3 = Arrays
								.stream(params)
								.filter(s -> s.toLowerCase().matches(".*number|total|amount|length.* \\d+"))
								.findFirst()
								.get();
							long val2 =
								Long.parseLong(stringVal2.substring(stringVal2.lastIndexOf(' ') + 1));
							long val3 =
								Long.parseLong(stringVal3.substring(stringVal3.lastIndexOf(' ') + 1));
							stringBuilder.append("media-id ").append(stringVal1).append('\n');
							stringBuilder.append("first-playback-piece ").append(val2).append('\n');
							stringBuilder.append("number-playback-pieces ").append(val3).append('\n');
						} catch (NoSuchElementException exception) {
							throw new IllegalStateException(exception);
						}
					} else {
						throw new IllegalStateException("Range not specified");
					}
				}
				default -> throw new IllegalStateException("Request type not specified");
			}
			request = stringBuilder.toString().getBytes();
			return new RubusRequest();
		}
	}

	private RubusRequest() {}

	public static RubusRequest.Builder newBuilder() {
		return new Builder();
	}

	public byte[] getBytes() {
		return null;
	}

}
