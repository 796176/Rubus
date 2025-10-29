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
import backend.exceptions.QueryingStrategyFactoryException;
import backend.querying.QueryingStrategyFactory;
import backend.querying.QueryingStrategyInterface;
import jakarta.annotation.Nonnull;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.function.Function;

public class QueryingStrategyFactoryStub implements QueryingStrategyFactory {

	public QueryingStrategyStub queryingStrategyStub = new QueryingStrategyStub();

	public Function<URI, QueryingStrategyInterface> getQueryingStrategyFunction = uri -> {
		throw new NotImplementedExceptions();
	};

	@Nonnull
	@Override
	public QueryingStrategyInterface getQueryingStrategy(@Nonnull URI uri) throws QueryingStrategyFactoryException {
		return getQueryingStrategyFunction.apply(uri);
	}

	public static class QueryingStrategyStub implements QueryingStrategyInterface {

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
			throw new NotImplementedExceptions();
		}

		@Nonnull
		@Override
		public SeekableByteChannel query(@Nonnull String name) {
			throw new NotImplementedExceptions();
		}

		@Nonnull
		@Override
		public SeekableByteChannel[] query(@Nonnull String[] names) {
			throw new NotImplementedExceptions();
		}

		@Override
		public void close() {
			throw new NotImplementedExceptions();
		}
	}
}
