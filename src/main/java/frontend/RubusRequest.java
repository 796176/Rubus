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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;

/**
 * RubusRequest is an auxiliary class designed to simplify construction of rubus request messages.
 */
public class RubusRequest {

	/**
	 * RubusRequest.Builder provides an interface to construct a new request message.
	 */
	public static class Builder {

		private final Logger logger = LoggerFactory.getLogger(Builder.class);

		private RubusRequestType requestType;

		private byte[] request = null;

		private final ArrayList<String> params = new ArrayList<>();

		private Builder() {
			logger.debug("{} instantiated", this);
		}

		/**
		 * Constructs the LIST type request message that includes all media titles.
		 * @return the current builder
		 */
		public Builder LIST() {
			return LIST(".*");
		}

		/**
		 * Constructs the LIST type request message with the specified search query.
		 * @param titleContains search query
		 * @return the current builder
		 */
		public Builder LIST(String titleContains) {
			requestType = RubusRequestType.LIST;
			params.add("title-contains " + titleContains);
			return this;
		}

		/**
		 * Constructs the INFO type request message.
		 * @param mediaId the media id
		 * @return the current builder
		 */
		public Builder INFO(String mediaId) {
			requestType = RubusRequestType.INFO;
			params.add("media-id " + mediaId);
			return this;
		}

		/**
		 * Constructs the FETCH type request message.
		 * @param mediaID the media id
		 * @param startingIndex the starting index of the playback clips
		 * @param amount the amount of playback clips to retrieve
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
		 * Constructs a RubusRequest instance using the state of this RubusRequest.Builder.
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

	private final Logger logger = LoggerFactory.getLogger(RubusRequest.class);

	private final byte[] request;

	private RubusRequest(byte[] request) {
		assert request != null;

		this.request = request;

		logger.debug("{} instantiated, request size: {}", this, request.length);
	}

	/**
	 * Constructs a new RubusRequest.Builder.
	 * @return a RubusRequest.Builder instance
	 */
	public static RubusRequest.Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Returns the request message represented as a byte array.
	 * @return the request message represented as a byte array
	 */
	public byte[] getBytes() {
		return request;
	}

}
