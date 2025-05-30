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

import java.util.HashMap;
import java.util.Map;

/**
 * PreProcessRequestParser is a concrete implementation of {@link RequestParserStrategy}. It parses the request
 * the moment it's fed and stores the key-value pairs. When the client queries a value PreProcessRequestParser returns it
 * without referring to the request.<br><br>
 *
 * PreProcessRequestParser vs {@link StandardRequestParser} comparison:<br>
 *     Memory usage: PreProcessRequestParser has to store both keys and values of the request making it much more memory
 *     inefficient than StandardRequestParser, which stores only the request reference.<br>
 *     Performance: asymptotically PreProcessRequestParser is more efficient because it parses the request once where
 *     the parsing algorithm complexity is O(n). Whereas StandardRequestParser has to parse it as many times as there are
 *     {@link #value(String)} invocations where each invocation is O(n). But this description doesn't show the full
 *     picture. The PreProcessRequestParser's parsing strategy is more expensive in terms of cycles: extracting not only
 *     values but also keys, memory allocation to store the pairs. That makes PreProcessorRequestParser, in fact, less
 *     efficient if the request is relatively small ( up to one to one and a half hundred characters ). Or if the client
 *     makes only few {@link #value(String)} invocations ( e.g. the client specified the key the request doesn't
 *     contain, and it's its first invocation after witch the client doesn't do any more invocations and just discards
 *     the entire request ) resulting in PrePrecessRequestParser being slower.
 */
public class PreProcessRequestParser implements RequestParserStrategy {

	private final static Logger logger = LoggerFactory.getLogger(PreProcessRequestParser.class);

	private final Map<String, String> parameters = new HashMap<>(50);

	private RubusRequestType type;

	public PreProcessRequestParser() {
		logger.debug("{} initialized", this);
	}

	@Override
	public void feed(String request) {
		assert request != null;

		parameters.clear();
		type = null;

		int offset = 0;
		while(true) {
			int nextSpaceIndex = request.indexOf(' ', offset);
			if (nextSpaceIndex == -1) return;
			int nextLFIndex = request.indexOf('\n', nextSpaceIndex);
			if (nextLFIndex == -1) throw new IllegalArgumentException();
			String key = request.substring(offset, nextSpaceIndex);
			String val = request.substring(nextSpaceIndex + 1, nextLFIndex);
			if (type == null) type = RubusRequestType.valueOf(val);
			else parameters.put(key, val);
			offset = nextLFIndex + 1;
		}
	}

	@Override
	public RubusRequestType type() {
		if (type == null) throw new IllegalArgumentException();
		return type;
	}

	@Override
	public String value(String key) {
		assert key != null;

		String val = parameters.get(key);
		if (val == null) throw new IllegalArgumentException();
		return val;
	}

	@Override
	public PreProcessRequestParser clone() {
		return new PreProcessRequestParser();
	}
}
