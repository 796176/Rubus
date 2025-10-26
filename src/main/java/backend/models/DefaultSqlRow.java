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

package backend.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * DefaultSqlRow is a concrete implementation of {@link SqlRow} that uses a {@link Map} for storing column values.
 * In addition to implementing the {@link SqlRow} interface, it defines a number of methods to fill the underlying map
 * with values.
 */
public class DefaultSqlRow implements SqlRow {

	private final Logger logger = LoggerFactory.getLogger(DefaultSqlRow.class);

	private final Map<String, Object> mapping = new HashMap<>();

	/**
	 * Constructs an instance of this class.
	 */
	public DefaultSqlRow() {
		logger.debug("{} instantiated", this);
	}

	/**
	 * Assigns the specified value to the column. If the column has been already assigned with an value, it's
	 * overridden.
	 * @param columnName the column name
	 * @param value the value
	 */
	public void putObject(@Nonnull String columnName, @Nullable Object value) {
		mapping.put(columnName, value);
	}

	@Override
	public int getInt(@Nonnull String columnName) {
		Object value = mapping.get(columnName);
		return value == null ? 0 : (int) value;
	}

	@Nullable
	@Override
	public String getString(@Nonnull String columnName) {
		return (String) mapping.get(columnName);
	}
}
