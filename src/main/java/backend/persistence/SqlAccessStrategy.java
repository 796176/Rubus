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

import backend.models.SqlRow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.sql.SQLException;
import java.util.stream.Stream;

/**
 * SqlAccessStrategy provides SQL-database-manufacturer-specific functionality.
 */
public interface SqlAccessStrategy {

	/**
	 * Queries the database for values associated with the specified columns.
	 * @param columnsNames an array of columns' names
	 * @return a stream of {@link SqlRow} instances
	 * @throws SQLException if querying fails
	 */
	@Nonnull
	Stream<SqlRow> query(@Nonnull String[] columnsNames) throws SQLException;

	/**
	 * Queries the database for values associated with the specified columns where the primary key column value matches
	 * the value in primaryKey. If the primary key column doesn't store the requested value, null is returned
	 * @param primaryKey the value that needs to match the database's primary key column value
	 * @param columnsNames an array of columns' names
	 * @return a single {@link SqlRow} instance, or null if it wasn't found
	 * @throws SQLException if querying fails
	 */
	@Nullable
	SqlRow query(@Nonnull String primaryKey, @Nonnull String[] columnsNames) throws SQLException;

	/**
	 * Queries the database for values associated with the specified columns and where the title value matches
	 * the search query.
	 * @param searchQuery the search query
	 * @param columnsNames an array of columns' names
	 * @return a stream of {@link SqlRow} instances
	 * @throws SQLException if querying fails
	 */
	@Nonnull
	Stream<SqlRow> searchInTitle(@Nonnull String searchQuery, @Nonnull String[] columnsNames) throws SQLException;
}
