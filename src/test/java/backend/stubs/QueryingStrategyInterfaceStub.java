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

package backend.stubs;

import backend.exceptions.NotImplementedExceptions;
import backend.exceptions.QueryingException;
import backend.querying.QueryingStrategyInterface;
import jakarta.annotation.Nonnull;

import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.function.Function;

public class QueryingStrategyInterfaceStub implements QueryingStrategyInterface {

	public Function<String[], SeekableByteChannel[]> queryFunction = names -> {
		throw new NotImplementedExceptions();
	};

	@Override
	public Object addToEnvironment(@Nonnull String name, Object value) {
		throw new NotImplementedExceptions();
	}

	@Override
	public Object removeFromEnvironment(@Nonnull String key) {
		throw new NotImplementedExceptions();
	}

	@Nonnull
	@Override
	public Map<String, Object> getEnvironment() {
		return Map.of();
	}

	@Nonnull
	@Override
	public SeekableByteChannel query(@Nonnull String name) throws QueryingException {
		return query(new String[] {name})[0];
	}

	@Nonnull
	@Override
	public SeekableByteChannel[] query(@Nonnull String[] names) throws QueryingException {
		return queryFunction.apply(names);
	}

	@Override
	public void close() {
		throw new NotImplementedExceptions();
	}
}
