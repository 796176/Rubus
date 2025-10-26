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

package backend.controllers;

import backend.authontication.Authenticator;
import backend.exceptions.InvalidParameterException;
import backend.interactors.MediaProvider;
import backend.models.*;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SeekableByteChannel;
import java.util.*;

/**
 * RequestProcessor defines a set of methods to process web requests and generate respective responses.
 * The methods are called by framework-extended classes and their results converted to appropriate formats.
 */
public class RequestProcessor {

	private final Logger logger = LoggerFactory.getLogger(RequestProcessor.class);

	private MediaProvider mediaProvider;

	private Authenticator authenticator;

	/**
	 * Constructs an instance of this class.
	 * @param mediaProvider the {@link MediaProvider} instance
	 * @param authenticator the {@link Authenticator} instance
	 */
	public RequestProcessor(
		@Nonnull MediaProvider mediaProvider,
		@Nonnull Authenticator authenticator

	) {
		setMediaProvider(mediaProvider);
		setAuthenticator(authenticator);

		logger.debug("{} instantiated, MediaProvider: {}, Authenticator: {}", this, mediaProvider, authenticator);
	}

	/**
	 * Requests media that match the specified search query.
	 * @param searchQuery the search query
	 * @param requestOriginator the client that made the request
	 * @return a {@link MediaList} instance
	 * @throws backend.exceptions.AuthenticationException if authentication fails
	 */
	public MediaList listRequest(@Nonnull String searchQuery, @Nonnull RequestOriginator requestOriginator) {
		Viewer viewer = authenticator.authenticate(requestOriginator);
		Media[] mediaArray = getMediaProvider().searchMedia(viewer, searchQuery);
		return new MediaList(
			Arrays
				.stream(mediaArray)
				.reduce(
					new HashMap<>(),
					(map, media)-> {
						map.put(media.getID(), media.getTitle());
						return map;
					},
					(map1, map2) -> {
						map1.putAll(map2);
						return map1;
					}
				)
		);
	}

	/**
	 * Requests additional information about the specified media.
	 * @param mediaId the media id associated with the media
	 * @param requestOriginator the client that made the request
	 * @return a {@link MediaInfo} instance
	 * @throws InvalidParameterException if the parameters are invalid
	 * @throws backend.exceptions.AuthenticationException if authentication fails
	 */
	public MediaInfo infoRequest(@Nonnull UUID mediaId, @Nonnull RequestOriginator requestOriginator) {
		Viewer viewer = authenticator.authenticate(requestOriginator);
		Media media = mediaProvider.getMedia(viewer, mediaId);
		if (media == null) throw new InvalidParameterException();
		return new MediaInfo(media.getID(), media.getTitle(), media.getDuration());
	}

	/**
	 * Requests content of the specified media.
	 * @param mediaId the media id associated with the media
	 * @param offset how many clips to skip
	 * @param amount the total amount of clips
	 * @param requestOriginator the client that made the request
	 * @return a {@link MediaFetch} instance
	 * @throws InvalidParameterException if the parameters are invalid
	 * @throws backend.exceptions.AuthenticationException if authentication fails
	 */
	public MediaFetch fetchRequest(
		@Nonnull UUID mediaId, int offset, int amount, @Nonnull RequestOriginator requestOriginator
	) throws InvalidParameterException {
		if (offset < 0 || amount <= 0 || offset + amount < 0) throw new InvalidParameterException();

		Viewer viewer = authenticator.authenticate(requestOriginator);
		Media media = mediaProvider.getMedia(viewer, mediaId);
		if (media == null || media.getDuration() < offset + amount) throw new InvalidParameterException();

		SeekableByteChannel[] audioClips = media.retrieveAudioClips(offset, amount);
		SeekableByteChannel[] videoClips = media.retrieveVideoClips(offset, amount);
		return new MediaFetch(media.getID(), offset, videoClips, audioClips);
	}

	/**
	 * Returns the current {@link Authenticator} instance.
	 * @return the current {@link Authenticator} instance
	 */
	@Nonnull
	public Authenticator getAuthenticator() {
		return authenticator;
	}

	/**
	 * Returns the current {@link MediaProvider} instance.
	 * @return the current {@link MediaProvider} instance.
	 */
	@Nonnull
	public MediaProvider getMediaProvider() {
		return mediaProvider;
	}

	/**
	 * Sets a new {@link Authenticator} instance.
	 * @param newAuthenticator a new {@link Authenticator} instance
	 */
	public void setAuthenticator(@Nonnull Authenticator newAuthenticator) {
		authenticator = newAuthenticator;
	}

	/**
	 * Sets a new {@link MediaProvider} instance.
	 * @param newMediaProvider a new {@link MediaProvider} instance
	 */
	public void setMediaProvider(@Nonnull MediaProvider newMediaProvider) {
		mediaProvider = newMediaProvider;
	}
}
