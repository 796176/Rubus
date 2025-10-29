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

package backend.querying;

import backend.exceptions.QueryingStrategyFactoryException;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class supports the following URI naming schemas: file. If none of the supported schemas works for the URI
 * it falls back to instantiating a {@link Path} instance using the URI's string representation, if it fails or the
 * resulted {@link Path} is not an existing directory, {@link QueryingStrategyInterface} is thrown. If the resulted
 * {@link Path} instance is an existing directory, an instance of {@link FSQueryingStrategy} is returned.
 */
public class DefaultQueryingStrategyFactory implements QueryingStrategyFactory {

	private final Logger logger = LoggerFactory.getLogger(DefaultQueryingStrategyFactory.class);

	public DefaultQueryingStrategyFactory() {
		logger.debug("{} instantiated", this);
	}

	@Nonnull
	@Override
	public QueryingStrategyInterface getQueryingStrategy(@Nonnull URI uri) throws QueryingStrategyFactoryException {
		try {
			switch (uri.getScheme()) {
				case "file" -> {
					return new FSQueryingStrategy(Path.of(uri.getPath()));
				}
				case null, default -> {
					Path path = Path.of(uri.toString());
					if (Files.exists(path) && Files.isDirectory(path)) return new FSQueryingStrategy(path);
				}
			}
		} catch (Exception e) {
			logger.error("{} encountered exception while instantiating querying strategy for {}", this, uri, e);
			throw new QueryingStrategyFactoryException(
				"Encountered an exception while instantiating querying strategy for " + uri, e
			);
		}
		logger.error("No QueryingStrategyInterface implementation exist for {}", uri);
		throw new QueryingStrategyFactoryException("No QueryingStrategyInterface implementation exist for " + uri);
	}
}
