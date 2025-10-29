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

import frontend.models.MediaFetch;
import frontend.models.MediaInfo;
import frontend.models.MediaList;

/**
 * RubusResponse provides methods to access the response type and the response objects.
 */
public interface RubusResponse {

	/**
	 * Returns the response type.
	 * @return the response type
	 */
	RubusResponseType getResponseType();

	/**
	 * Returns a {@link MediaList} instance, or null if the instance isn't present in the response message.
	 * @return a {@link MediaList} instance, or null if the instance isn't present in the response message
	 */
	MediaList LIST();

	/**
	 * Returns a {@link MediaInfo} instance, or null if the instance isn't present in the response message.
	 * @return a {@link MediaInfo} instance, or null if the instance isn't present in the response message
	 */
	MediaInfo INFO();

	/**
	 * Returns a {@link MediaFetch} instance, or null if the instance isn't present in the response message.
	 * @return a {@link MediaFetch} instance, or null if the instance isn't present in the response message
	 */
	MediaFetch FETCH();
}