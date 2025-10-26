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

/**
 * SqlRow represents a single SQL row and gives access to its values.
 */
public interface SqlRow {

	/**
	 * Returns the value of the specified column. If the value is null, 0 is returned.
	 * @param columnName the column name
	 * @return the value of the specified column
	 */
	int getInt(@Nonnull String columnName);

	/**
	 * Returns the value of the specified column.
	 * @param columnName the column name
	 * @return the value of the specified column
	 */
	@Nullable
	String getString(@Nonnull String columnName);
}
