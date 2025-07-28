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

package auxiliary;

import backend.RequestParserStrategy;
import common.net.request.RubusRequestType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

public class RequestParserStrategyStub implements RequestParserStrategy {

	public RubusRequestType requestType;

	public Map<String, String> pairs;

	public RequestParserStrategyStub(RubusRequestType requestType, Map<String, String> keyValuePairs) {
		this.requestType = requestType;
		pairs = keyValuePairs;
	}

	@Override
	public void feed(String request) { }

	@Override
	public RubusRequestType type() {
		return requestType;
	}

	@Override
	public String value(String key) {
		if (pairs == null) return null;
		if (pairs.containsKey(key)) return pairs.get(key);
		fail("Key: " + key + " isn't present");
		return null;
	}

	@Override
	public RequestParserStrategy clone() {
		return new RequestParserStrategyStub(requestType, pairs);
	}
}
