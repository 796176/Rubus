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

import backend.exceptions.AuthorizationException;
import backend.models.Media;
import backend.models.Viewer;
import backend.authorization.ActionType;
import backend.authorization.ViewerAuthorizer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * A concrete implementation of {@link MediaProvider} that performs authorization before serving a request.
 * Authorization is delegated to a {@link ViewerAuthorizer} instance.
 */
public class DefaultMediaProvider implements MediaProvider {

	private final Logger logger = LoggerFactory.getLogger(DefaultMediaProvider.class);

	private final ViewerAuthorizer viewerAuthorizer;

	private final MediaDataAccess mediaDataAccess;

	/**
	 * Constructs an instance of this class.
	 * @param viewerAuthorizer the {@link ViewerAuthorizer} instance
	 * @param mediaDataAccess the {@link MediaDataAccess} instance
	 */
	public DefaultMediaProvider(ViewerAuthorizer viewerAuthorizer, MediaDataAccess mediaDataAccess) {
		this.viewerAuthorizer = viewerAuthorizer;
		this.mediaDataAccess = mediaDataAccess;

		logger.debug(
			"{} instantiated, ViewerAuthorizer: {}, MediaDataAccess: {}", this, viewerAuthorizer, mediaDataAccess
		);
	}

	/**
	 * Returns a single media associated with the provided media id.
	 * @param viewer the viewer making the request
	 * @param mediaId the media id
	 * @return an instance of {@link Media} or null if no media is associated with the provided id
	 * @throws AuthorizationException if the viewer is not permitted to access the requested media
	 * @throws backend.exceptions.CommonDataAccessException if the underlying datastore is not accessible
	 */
	@Nullable
	@Override
	public Media getMedia(@Nonnull Viewer viewer, @Nonnull UUID mediaId) {
		if (!viewerAuthorizer.validate(viewer, ActionType.READ)) {
			throw new AuthorizationException("The viewer " + viewer + " isn't authorized");
		}
		return mediaDataAccess.getMedia(mediaId);
	}

	/**
	 * Returns all media.
	 * @param viewer the viewer making the request
	 * @return an array of {@link Media} instances
	 * @throws AuthorizationException if the viewer is not permitted to access the requested media
	 * @throws backend.exceptions.CommonDataAccessException if the underlying datastore is not accessible
	 */
	@Nonnull
	@Override
	public Media[] getMedia(@Nonnull Viewer viewer) {
		if (!viewerAuthorizer.validate(viewer, ActionType.READ)) {
			throw new AuthorizationException("The viewer " + viewer + " isn't authorized");
		}
		return mediaDataAccess.getMedia();
	}

	/**
	 * Returns media that match the search query.
	 * @param viewer the viewer making the request
	 * @param searchQuery the search query
	 * @return an array of {@link Media} instances that match the search query
	 * @throws AuthorizationException if the viewer is not permitted to access the requested media
	 * @throws backend.exceptions.CommonDataAccessException if the underlying datastore is not accessible
	 */
	@Nonnull
	@Override
	public Media[] searchMedia(@Nonnull Viewer viewer, @Nonnull String searchQuery) {
		if (!viewerAuthorizer.validate(viewer, ActionType.READ)) {
			throw new AuthorizationException("The viewer " + viewer + "isn't authorized");
		}
		return mediaDataAccess.searchMedia(searchQuery);
	}
}
