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

package common.net;

import java.io.IOException;

/**
 * This exception is thrown by {@link frontend.FetchController} when it couldn't retrieve the playback pieces from
 * the server.
 */
public class FetchingException extends IOException {

	/**
	 * Constructs an instance of this class.
	 * @param message the detail message
	 */
	public FetchingException(String message) {
		super(message);
	}
}
