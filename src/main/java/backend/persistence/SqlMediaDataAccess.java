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

package backend.persistence;

import backend.exceptions.CommonDataAccessException;
import backend.exceptions.CorruptedDataException;
import backend.exceptions.QueryingStrategyFactoryException;
import backend.interactors.MediaDataAccess;
import backend.models.Media;
import backend.models.DefaultMedia;
import backend.models.SqlRow;
import backend.querying.QueryingStrategyFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * SqlMediaDataAccess uses a SQL database as its storage facility without relying on a specific database manufacturer.
 * The SQL syntax may vary from one database manufacturer to another, because of that SqlMediaDataAccess doesn't access
 * a database directly nor construct SQL queries. It only defines an application-specific SQL schema and delegates
 * the reset of the functionality to a {@link SqlAccessStrategy} instance.
 */
public class SqlMediaDataAccess implements MediaDataAccess {

	private final Logger logger = LoggerFactory.getLogger(SqlMediaDataAccess.class);

	private QueryingStrategyFactory queryingStrategyFactory;

	private SqlAccessStrategy sqlAccessStrategy;

	/**
	 * Constructs an instance of this class.
	 * @param queryingStrategyFactory the {@link QueryingStrategyFactory} instance that instantiates a respective
	 *                                {@link backend.querying.QueryingStrategyInterface} for {@link Media} instance
	 * @param sqlAccessStrategy the {@link SqlAccessStrategy} instance
	 */
	public SqlMediaDataAccess(
		@Nonnull QueryingStrategyFactory queryingStrategyFactory, @Nonnull SqlAccessStrategy sqlAccessStrategy
	) {
		setQueryingStrategyFactory(queryingStrategyFactory);
		setSqlAccessStrategy(sqlAccessStrategy);

		logger.debug(
			"{} instantiated, QueryStrategyFactory: {}, SqlAccessStrategy: {}",
			this,
			queryingStrategyFactory,
			sqlAccessStrategy
		);
	}

	@Nonnull
	@Override
	public Media[] getMedia() throws CommonDataAccessException {
		try {
			String[] schema = new String[] {"id", "title", "duration", "media_content_uri"};
			return sqlAccessStrategy
				.query(schema)
				.map(sqlRow -> {
					try {
						URI uri = URI.create(requireNonNull(sqlRow.getString("media_content_uri")));
						return new DefaultMedia(
							UUID.fromString(requireNonNull(sqlRow.getString("id"))),
							requireNonNull(sqlRow.getString("title")),
							requirePositive(sqlRow.getInt("duration")),
							uri,
							getQueryingStrategyFactory().getQueryingStrategy(uri)
						);
					} catch (NullPointerException | IllegalArgumentException | QueryingStrategyFactoryException e) {
						logger.error(
							"{} encountered unexpected value in {} schema", this, Arrays.toString(schema), e
						);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.toArray(Media[]::new);
		} catch (Exception e) {
			throw new CommonDataAccessException(e);
		}
	}

	@Nullable
	@Override
	public Media getMedia(@Nonnull UUID mediaId) throws CommonDataAccessException {
		String[] schema = new String[] {"id", "title", "duration", "media_content_uri"};
		try {
			SqlRow row = sqlAccessStrategy.query(mediaId.toString(), schema);
			if (row == null) return null;
			URI uri = URI.create(requireNonNull(row.getString("media_content_uri")));
			return new DefaultMedia(
				UUID.fromString(requireNonNull(row.getString("id"))),
				requireNonNull(row.getString("title")),
				requirePositive(row.getInt("duration")),
				uri,
				getQueryingStrategyFactory().getQueryingStrategy(uri)
			);
		} catch (NullPointerException | IllegalArgumentException | QueryingStrategyFactoryException e) {
			logger.error("{} encountered unexpected value in {} schema", this, Arrays.toString(schema), e);
			throw new CorruptedDataException(e);
		} catch (Exception e) {
			throw new CommonDataAccessException(e);
		}
	}

	@Nonnull
	@Override
	public Media[] searchMedia(@Nonnull String searchQuery) throws CommonDataAccessException {
		try {
			String[] schema = new String[] {"id", "title", "duration", "media_content_uri"};
			return sqlAccessStrategy
				.searchInTitle(searchQuery, schema)
				.map(sqlRow -> {
					try {
						URI uri = URI.create(requireNonNull(sqlRow.getString("media_content_uri")));
						return new DefaultMedia(
							UUID.fromString(requireNonNull(sqlRow.getString("id"))),
							requireNonNull(sqlRow.getString("title")),
							requirePositive(sqlRow.getInt("duration")),
							uri,
							getQueryingStrategyFactory().getQueryingStrategy(uri)
						);
					} catch (NullPointerException | IllegalArgumentException | QueryingStrategyFactoryException e) {
						logger.error(
							"{} encountered unexpected value in {} schema", this, Arrays.toString(schema), e
						);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.toArray(Media[]::new);
		} catch (Exception e) {
			throw new CommonDataAccessException(e);
		}
	}

	/**
	 * Returns the current {@link QueryingStrategyFactory} instance.
	 * @return the current {@link QueryingStrategyFactory} instance
	 */
	public QueryingStrategyFactory getQueryingStrategyFactory() {
		return queryingStrategyFactory;
	}

	/**
	 * Returns the current {@link SqlAccessStrategy} instance.
	 * @return the current {@link SqlAccessStrategy} instance
	 */
	public SqlAccessStrategy getSqlAccessStrategy() {
		return sqlAccessStrategy;
	}

	/**
	 * Sets a new {@link QueryingStrategyFactory} instance.
	 * @param newQueryingStrategyFactory a new {@link QueryingStrategyFactory} instance
	 */
	public void setQueryingStrategyFactory(@Nonnull QueryingStrategyFactory newQueryingStrategyFactory) {
		queryingStrategyFactory = newQueryingStrategyFactory;
	}

	/**
	 * Sets a new {@link SqlAccessStrategy} instance.
	 * @param newSqlAccessStrategy a new {@link SqlAccessStrategy} instance
	 */
	public void setSqlAccessStrategy(@Nonnull SqlAccessStrategy newSqlAccessStrategy) {
		sqlAccessStrategy = newSqlAccessStrategy;
	}

	private int requirePositive(int i) {
		if (i <= 0) throw new IllegalArgumentException();
		return i;
	}
}
