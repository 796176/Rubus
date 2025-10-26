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

package backend.interactors;

import backend.exceptions.CommonSecurityException;
import backend.models.Media;
import backend.models.Viewer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * MediaProvider serves media to viewers.
 */
public interface MediaProvider {

	/**
	 * Returns a single media associated with the provided media id.
	 * @param viewer the viewer making the request
	 * @param mediaId the media id
	 * @return an instance of {@link Media} or null if no media is associated with the provided id
	 * @throws CommonSecurityException if the viewer is not permitted to access the requested media
	 */
	@Nullable
	Media getMedia(@Nonnull Viewer viewer, @Nonnull UUID mediaId) throws CommonSecurityException;

	/**
	 * Returns all media.
	 * @param viewer the viewer making the request
	 * @return an array of {@link Media} instances
	 * @throws CommonSecurityException if the viewer is not permitted to access the requested media
	 */
	@Nonnull
	Media[] getMedia(@Nonnull Viewer viewer) throws CommonSecurityException;

	/**
	 * Returns media that match the search query.
	 * @param viewer the viewer making the request
	 * @param searchQuery the search query
	 * @return an array of {@link Media} instances that match the search query
	 * @throws CommonSecurityException if the viewer is not permitted to access the requested media
	 */
	@Nonnull
	Media[] searchMedia(@Nonnull Viewer viewer, @Nonnull String searchQuery) throws CommonSecurityException;

}
