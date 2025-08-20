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

import backend.querying.QueryingStrategyInterface;

import java.nio.channels.SeekableByteChannel;
import java.util.Map;

public class QueryStrategyStub implements QueryingStrategyInterface {
	@Override
	public Object addToEnvironment(String name, Object value) {
		return null;
	}

	@Override
	public Object removeFromEnvironment(String key) {
		return null;
	}

	@Override
	public Map<String, Object> getEnvironment() {
		return Map.of();
	}

	@Override
	public SeekableByteChannel query(String name) {
		return null;
	}

	@Override
	public SeekableByteChannel[] query(String[] names) {
		return new SeekableByteChannel[0];
	}

	@Override
	public void close() throws Exception {

	}
}
