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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StandardRequestParser is a concrete implementation of {@link RequestParserStrategy}, where parsing is only performed
 * when calling {@link #type()} and {@link #value(String)}. The results returned by these methods are not cached
 * locally.
 */
public class StandardRequestParser implements RequestParserStrategy {

	private final static Logger logger = LoggerFactory.getLogger(StandardRequestParser.class);

	private String request;

	public StandardRequestParser() {
		logger.debug("{} instantiated", this);
	}

	@Override
	public void feed(String request) {
		assert request != null;

		this.request = request;
	}

	@Override
	public RubusRequestType type() {
		if (request == null) throw new IllegalArgumentException();
		try {
			return RubusRequestType.valueOf(request.substring(request.indexOf(' ') + 1, request.indexOf('\n')));
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public String value(String key) {
		assert key != null;

		int index = request.indexOf(key);
		if (index == -1) throw new IllegalArgumentException();
		return request.substring(index + key.length() + 1, request.indexOf('\n', index));
	}

	@Override
	public StandardRequestParser clone() {
		return new StandardRequestParser();
	}
}
