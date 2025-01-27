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

import java.util.Arrays;
import java.util.NoSuchElementException;

public class RubusRequest {
	public static class Builder {
		private Builder() {}

		private enum Request {
			LIST, INFO, FETCH
		}

		Request requestType;

		String[] params = null;

		byte[] request = null;

		Builder LIST() {
			requestType = Request.LIST;
			return this;
		}

		Builder INFO() {
			requestType = Request.INFO;
			return this;
		}

		Builder FETCH() {
			requestType = Request.FETCH;
			return this;
		}


		Builder params(String... params) {
			this.params = params;
			return this;
		}

		RubusRequest build() throws IllegalStateException {
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
