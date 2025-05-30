/*
 * Rubus is an application layer protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024-2025 Yegore Vlussove
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

import java.util.ArrayList;
import java.util.Collections;

/**
 * RubusRequest is an auxiliary class designed to simplify the construction of rubus requests.
 */
public class RubusRequest {

	/**
	 * RubusRequest.Builder provide an interface to construct a new request.
	 */
	public static class Builder {
		private Builder() {}

		private RubusRequestType requestType;

		private byte[] request = null;

		private final ArrayList<String> params = new ArrayList<>();

		/**
		 * Constructs the LIST type request with no title filter.
		 * @return the current builder
		 */
		public Builder LIST() {
			return LIST(".*");
		}

		/**
		 * Constructs the LIST type request with the specified title filter.
		 * @param titleContains request for titles containing the string
		 * @return the current builder
		 */
		public Builder LIST(String titleContains) {
			requestType = RubusRequestType.LIST;
			params.add("title-contains " + titleContains);
			return this;
		}

		/**
		 * Constructs the INFO type request.
		 * @param mediaId the media id
		 * @return the current builder
		 */
		public Builder INFO(String mediaId) {
			requestType = RubusRequestType.INFO;
			params.add("media-id " + mediaId);
			return this;
		}

		/**
		 * Constructs the FETCH type request.
		 * @param mediaID the media id
		 * @param startingIndex the starting index of the playback piece
		 * @param amount the amount of playback pieces to request
		 * @return the current builder
		 */
		public Builder FETCH(String mediaID, int startingIndex, int amount) {
			requestType = RubusRequestType.FETCH;
			params.add("media-id " + mediaID);
			params.add("starting-playback-piece " + startingIndex);
			params.add("total-playback-pieces " + amount);
			return this;
		}

		/**
		 * Adds parameters to the request.
		 * @param extraParameters extra parameters
		 * @return the current builder
		 */
		public Builder addParameters(String... extraParameters) {
			Collections.addAll(this.params, extraParameters);
			return this;
		}

		/**
		 * Builds the request using the set type and parameters
		 * @return a RubusRequest instance
		 * @throws IllegalStateException if parameters are not acceptable
		 */
		public RubusRequest build() throws IllegalStateException {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("request-type ").append(requestType);
			for (String param: params) {
				stringBuilder.append('\n').append(param);
			}
			stringBuilder.append("\nbody-length 0\n\n");
			request = stringBuilder.toString().getBytes();
			return new RubusRequest(request);
		}
	}

	private final byte[] request;

	private RubusRequest(byte[] request) {
		assert request != null;

		this.request = request;
	}

	/**
	 * Constructs a new RubusRequest.Builder.
	 * @return a Builder instance
	 */
	public static RubusRequest.Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Returns the built request represented as a byte array.
	 * @return the built request represented as a byte array
	 */
	public byte[] getBytes() {
		return request;
	}

}
