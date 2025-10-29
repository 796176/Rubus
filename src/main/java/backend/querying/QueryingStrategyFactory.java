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

import java.net.URI;

/**
 * QueryingStrategyFactory is an auxiliary interface for implementations of {@link QueryingStrategyInterface} that (1)
 * have a hierarchical resource structure, and (2) the queried resource are located under a single parent.
 * QueryingStrategyFactory instantiates a {@link QueryingStrategyInterface} instance using the URI of the parent.
 */
public interface QueryingStrategyFactory {

	/**
	 * Instantiates and returns a {@link QueryingStrategyInterface} instance based on the provided URI. If method
	 * cannot instantiate a strategy or the instantiation fails this method throws
	 * {@link QueryingStrategyFactoryException}.
	 * @param uri the URI
	 * @return a {@link QueryingStrategyInterface} instance
	 * @throws QueryingStrategyFactoryException if instantiation fails
	 */
	@Nonnull
	QueryingStrategyInterface getQueryingStrategy(@Nonnull URI uri) throws QueryingStrategyFactoryException;
}
