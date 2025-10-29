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

package backend.interactors;

import backend.exceptions.CommonDataAccessException;
import backend.models.Media;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * MediaDataAccess describes a read-only access interface of underlying storage facilities of {@link Media} instances.
 */
public interface MediaDataAccess {

	/**
	 * Returns all media.
	 * @return an array of {@link Media} instances
	 * @throws CommonDataAccessException if the storage facilities can't be accessed
	 */
	@Nonnull
	Media[] getMedia() throws CommonDataAccessException;

	/**
	 * Returns the {@link Media} instance associated with the specified media id. If there is no media associated with
	 * the id, null is returned.
	 * @param mediaId a single media associated with the provided media id.
	 * @return the {@link Media} instance or null if no media is associated with the provided id
	 * @throws CommonDataAccessException if the storage facilities can't be accessed
	 */
	@Nullable
	Media getMedia(@Nonnull UUID mediaId) throws CommonDataAccessException;

	/**
	 * Returns media that match the search query.
	 * @param searchQuery the search query
	 * @return an array of {@link Media} instances that match the search query
	 * @throws CommonDataAccessException if the storage facilities can't be accessed
	 */
	@Nonnull
	Media[] searchMedia(@Nonnull String searchQuery) throws CommonDataAccessException;
}
