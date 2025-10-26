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

package backend.stubs;

import backend.models.SqlRow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;

public class SqlRowStub implements SqlRow {

	public HashMap<String, Object> hashMap = new HashMap<>();

	@Override
	public int getInt(@Nonnull String columnName) {
		return hashMap.get(columnName) == null ? 0 : (int) hashMap.get(columnName);
	}

	@Nullable
	@Override
	public String getString(@Nonnull String columnName) {
		return (String) hashMap.get(columnName);
	}
}
