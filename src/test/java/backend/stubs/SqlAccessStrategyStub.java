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
import backend.models.SqlRow;
import backend.persistence.SqlAccessStrategy;
import jakarta.annotation.Nonnull;

import java.sql.SQLException;
import java.util.stream.Stream;

public class SqlAccessStrategyStub implements SqlAccessStrategy {

	public interface Function <T, R> {

		R apply(T t) throws SQLException;
	}

	public interface BiFunction<T, U, R> {

		R apply(T t, U u) throws SQLException;
	}

	public Function<String[], Stream<SqlRow>> queryMultipleSqlRowsFunction = columns -> {
		throw new NotImplementedExceptions();
	};

	public BiFunction<String, String[], SqlRow> querySingleSqlRowFunction = (key, columns) -> {
		throw new NotImplementedExceptions();
	};

	public BiFunction<String, String[], Stream<SqlRow>> searchItTitleFunction = (query, columns) -> {
		throw new NotImplementedExceptions();
	};

	@Nonnull
	@Override
	public Stream<SqlRow> query(@Nonnull String[] columnsNames) throws SQLException {
		return queryMultipleSqlRowsFunction.apply(columnsNames);
	}

	@Override
	public SqlRow query(@Nonnull String primaryKey, @Nonnull String[] columnsNames) throws SQLException {
		return querySingleSqlRowFunction.apply(primaryKey, columnsNames);
	}

	@Nonnull
	@Override
	public Stream<SqlRow> searchInTitle(
		@Nonnull String searchQuery, @Nonnull String[] columnsNames
	) throws SQLException {
		return searchItTitleFunction.apply(searchQuery, columnsNames);
	}
}
