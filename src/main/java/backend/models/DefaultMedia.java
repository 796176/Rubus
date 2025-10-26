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

import backend.exceptions.QueryingException;
import backend.querying.QueryingStrategyInterface;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;

/**
 * A concrete implementation of {@link Media}.
 */
public class DefaultMedia implements Media {

	private final static Logger logger = LoggerFactory.getLogger(DefaultMedia.class);

	private final UUID id;

	private final String title;

	private final int duration;

	private final URI contentUri;

	private QueryingStrategyInterface qsi;

	/**
	 * Constructs an instance of this class.
	 * @param id the media id
	 * @param title the title
	 * @param duration the duration
	 * @param contentUri the URI of the media content
	 * @param queryingStrategyInterface the querying strategy to retrieve the media content ( e.g. video, audio )
	 */
	public DefaultMedia(
		@Nonnull UUID id,
		@Nonnull String title,
		int duration,
		@Nonnull URI contentUri,
		@Nonnull QueryingStrategyInterface queryingStrategyInterface
	) {
		assert duration > 0;

		this.id = id;
		this.title = title;
		this.duration = duration;
		this.contentUri = contentUri;
		setQueryingStrategy(queryingStrategyInterface);

		logger.debug(
			"{} instantiated, id: {}, title: {}, duration: {} QueryingStrategyInterface: {}",
			this,
			id,
			title,
			duration,
			queryingStrategyInterface
		);
	}

	@Nonnull
	@Override
	public UUID getID() {
		return id;
	}

	@Nonnull
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Nonnull
	@Override
	public URI getContentURI() {
		return contentUri;
	}

	/**
	 * Retrieves audio clips of the specified range.
	 * @param offset how many clips to skip
	 * @param amount how many clips to retrieve
	 * @return an ordered array of {@link SeekableByteChannel} instances containing audio clips
	 * @throws QueryingException if querying fails
	 */
	@Nonnull
	@Override
	public SeekableByteChannel[] retrieveAudioClips(int offset, int amount) throws QueryingException {
		assert offset >= 0 && amount > 0 && offset + amount <= getDuration();

		String[] audioClipsNames = new String[amount];
		for (int arrayIndex = 0; arrayIndex < audioClipsNames.length; arrayIndex++) {
			audioClipsNames[arrayIndex] = "a" + (arrayIndex + offset);
		}
		return qsi.query(audioClipsNames);
	}

	/**
	 * Retrieves video clips of the specified range.
	 * @param offset how many clips to skip
	 * @param amount how many clips to retrieve
	 * @return an ordered array of {@link SeekableByteChannel} instances containing video clips
	 * @throws QueryingException if querying fails
	 */
	@Nonnull
	@Override
	public SeekableByteChannel[] retrieveVideoClips(int offset, int amount) throws QueryingException {
		assert offset >= 0 && amount > 0 && offset + amount <= getDuration();

		String[] videoClipsNames = new String[amount];
		for (int arrayIndex = 0; arrayIndex < videoClipsNames.length; arrayIndex++) {
			videoClipsNames[arrayIndex] = "v" + (arrayIndex + offset);
		}
		return qsi.query(videoClipsNames);
	}

	/**
	 * Returns the current {@link QueryingStrategyInterface} instance.
	 * @return the current {@link QueryingStrategyInterface} instance
	 */
	@Nonnull
	public QueryingStrategyInterface getQueryingStrategy() {
		return qsi;
	}

	/**
	 * Sets a new {@link QueryingStrategyInterface} instance.
	 * @param newQueryingStrategy a new {@link QueryingStrategyInterface} instance
	 */
	public void setQueryingStrategy(@Nonnull QueryingStrategyInterface newQueryingStrategy) {
		qsi = newQueryingStrategy;
	}
}
