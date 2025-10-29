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

/**
 * Concrete implementations of this interface provide necessary information accessible to respective {@link RubusClient}
 * implementation classes.
 */
public interface RubusRequest {

	/**
	 * RubusRequest.Builder provides an interface to construct a new request message.
	 */
	interface Builder {

		/**
		 * Sets the remote host name.
		 * @param host the remote host name
		 * @return the current builder
		 */
		Builder host(@Nonnull String host);

		/**
		 * Sets the remote port.
		 * @param port the remote port
		 * @return the current builder
		 */
		Builder port(int port);

		/**
		 * Assigns this request type to the LIST request type with an empty search query.
		 * @return the current builder
		 */
		Builder LIST();

		/**
		 * Assigns this request type to the LIST request type with the specified search query.
		 * @param searchQuery the search query
		 * @return the current builder
		 */
		Builder LIST(@Nonnull String searchQuery);

		/**
		 * Assigns this request type to the INFO request type with the provided media id.
		 * @param mediaId the media id
		 * @return the current builder
		 */
		Builder INFO(@Nonnull String mediaId);

		/**
		 * Assigns this request type to the FETCH request type with the provided parameters.
		 * @param mediaID the media id
		 * @param offset how many clips to skip
		 * @param amount how many clips to retrieve
		 * @return the current builder
		 */
		Builder FETCH(@Nonnull String mediaID, int offset, int amount);

		/**
		 * Constructs a RubusRequest instance using the state of this RubusRequest.Builder.
		 * @return a RubusRequest instance
		 * @throws IllegalStateException if this builder is misconfigured
		 */
		RubusRequest build() throws IllegalStateException;
	}
}