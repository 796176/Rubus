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

package common.net.response;

/**
 * Every response message needs to be one of the following types.
 */
public enum RubusResponseType {

	/**
	 * The OK type specified that the request was handled appropriately and the response contains the data required
	 * by the client.
	 */
	OK,

	/**
	 * The BAD_REQUEST type specifies that the request message was not acceptable.
	 */
	BAD_REQUEST,

	/**
	 * The SERVER_ERROR type specifies that the request wasn't handled appropriately because of a server error.
	 */
	SERVER_ERROR
}
