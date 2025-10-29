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

import backend.models.DefaultSqlRow;
import backend.models.SqlRow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * PostgresAccessStrategy implements its functionality by relying on PostgreSQL; no other services are required.
 * The full text search is provided by Postgres with the syntax of the search query described
 * <a href="https://www.postgresql.org/docs/current/textsearch-controls.html#TEXTSEARCH-PARSING-QUERIES">here</a>.
 */
@Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
public class PostgresAccessStrategy implements SqlAccessStrategy {

	private final Logger logger = LoggerFactory.getLogger(PostgresAccessStrategy.class);

	private JdbcTemplate jdbcTemplate;

	/**
	 * Creates an instance of this class.
	 * @param jdbcTemplate the {@link JdbcTemplate} instance connected to a database containing the 'media' table
	 */
	public PostgresAccessStrategy(@Nonnull JdbcTemplate jdbcTemplate) {
		setJdbcTemplate(jdbcTemplate);

		logger.debug("{} instantiated, JdbcTemplate: {}", this, jdbcTemplate);
	}

	@Nonnull
	@Override
	public Stream<SqlRow> query(@Nonnull String[] columnsNames) {
		assert columnsNames.length > 0;

		StringBuilder sqlQuery = new StringBuilder("SELECT ");
		for (int i = 0; i < columnsNames.length; i++) {
			sqlQuery.append(columnsNames[i]);
			if (i != columnsNames.length - 1)  sqlQuery.append(", ");
		}
		sqlQuery.append(" FROM media;");

		logger.info("{} executing {}", this, sqlQuery);

		return Objects.requireNonNullElse(
			jdbcTemplate.query(sqlQuery.toString(), rs-> {
				ArrayList<SqlRow> result = new ArrayList<>();
				while (rs.next()) {
					DefaultSqlRow sqlRow = new DefaultSqlRow();
					for (String column: columnsNames) {
						sqlRow.putObject(column, rs.getObject(column));
					}
					result.add(sqlRow);
				}
				return result.stream();
			}),
			Stream.empty()
		);
	}

	@Nullable
	@Override
	public SqlRow query(@Nonnull String primaryKey, @Nonnull String[] columnsNames) throws SQLException {
		assert columnsNames.length > 0;

		StringBuilder sqlQuery = new StringBuilder("SELECT ");
		for (int i = 0; i < columnsNames.length; i++) {
			sqlQuery.append(columnsNames[i]);
			if (i != columnsNames.length - 1) sqlQuery.append(", ");
		}
		sqlQuery.append(" FROM media WHERE id=?;");

		return jdbcTemplate.query(
			sqlQuery.toString(),
			preparedStatement-> {
				preparedStatement.setString(1, primaryKey);
				logger.info("{} executing {}", this, preparedStatement);
			},
			rs -> {
				if (!rs.next()) return null;
				DefaultSqlRow sqlRow = new DefaultSqlRow();
				for (String columnName: columnsNames) {
					sqlRow.putObject(columnName, rs.getObject(columnName));
				}
				return sqlRow;
			}
		);
	}

	@Nonnull
	@Override
	public Stream<SqlRow> searchInTitle(@Nonnull String searchQuery, @Nonnull String[] columnsNames) {
		assert columnsNames.length > 0;

		StringBuilder sqlQuery = new StringBuilder("""
			WITH w (search_query, search_query_size) AS \
			(SELECT temp, numnode(temp) FROM (SELECT websearch_to_tsquery('english', ?) AS temp)) \
			SELECT""").append(" ");
		for (int i = 0; i < columnsNames.length; i++) {
			sqlQuery.append(columnsNames[i]);
			if (i != columnsNames.length - 1) sqlQuery.append(", ");
		}
		sqlQuery.append("""
			 FROM media WHERE \
			((SELECT search_query FROM w) @@ title_tsvector) OR ((SELECT search_query_size FROM w) = 0);""");

		return Objects.requireNonNullElse(
			jdbcTemplate.query(
				sqlQuery.toString(),
				preparedStatement -> {
					preparedStatement.setString(1, searchQuery);
					logger.info("{} executing {}", this, preparedStatement);
				},
				rs -> {
					ArrayList<SqlRow> result = new ArrayList<>();
					while (rs.next()) {
						DefaultSqlRow sqlRow = new DefaultSqlRow();
						for (String column: columnsNames) {
							sqlRow.putObject(column, rs.getObject(column));
						}
						result.add(sqlRow);
					}
					return result.stream();
				}
			),
			Stream.empty()
		);
	}

	/**
	 * Returns the current {@link JdbcTemplate} instance.
	 * @return the current {@link JdbcTemplate} instance
	 */
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	/**
	 * Sets a new {@link JdbcTemplate} instance.
	 * @param newJdbcTemplate a new {@link JdbcTemplate} instance
	 */
	public void setJdbcTemplate(@Nonnull JdbcTemplate newJdbcTemplate) {
		jdbcTemplate = newJdbcTemplate;
	}
}
