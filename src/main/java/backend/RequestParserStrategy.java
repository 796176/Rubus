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

package backend;

import common.net.request.RubusRequestType;

/**
 * RequestParserStrategy provides an interface the client uses to parse incoming request messages. All concrete
 * implementations of this interface allow for reusing of the same instance to parse different messages unless otherwise
 * is specified.
 */
public interface RequestParserStrategy {

	/**
	 * Accepts a new request message this instance needs to parse. After invoking this method, {@link #type()} and
	 * {@link #value(String)} can be called to access the message's content.
	 * @param request a new request message to parse
	 */
	void feed(String request);

	/**
	 * Returns the request type.
	 * @return the request type
	 * @throws IllegalArgumentException if the message doesn't contain a request type
	 */
	RubusRequestType type();

	/**
	 * Returns the value associated with the specified key.
	 * @param key the key
	 * @return the value
	 * @throws IllegalArgumentException if the message doesn't contain a field with the specified key
	 */
	String value(String key);

	/**
	 * Constructs a new instance of RequestParserStrategy by prototyping the concrete implementation of this interface.
	 * @return a new instance of RequestParserStrategy
	 */
	RequestParserStrategy clone();
}
