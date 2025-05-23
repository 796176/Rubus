/*
 * Rubus is an application level protocol for video and audio streaming and
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

package common.net.request;

/**
 * Every request message needs to be one of the following types.
 */
public enum RubusRequestType {

	/**
	 * The LIST type requests the list of media available on the server.
	 */
	LIST,

	/**
	 * The INFO type requests the meta-information of the specified media.
	 */
	INFO,

	/**
	 * The FETCH type requests a range of playback pieces of the specified media.
	 */
	FETCH
}
