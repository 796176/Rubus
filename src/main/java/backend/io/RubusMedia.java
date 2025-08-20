/*
 * Rubus is an application layer protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024-2025 Yegore Vlussove
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

package backend.io;

import backend.querying.QueryingStrategyInterface;
import common.net.response.body.MediaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;

/**
 * RubusMedia is a concrete implementation of {@link Media} that stores media-specific information.
 */
public class RubusMedia implements Media {

	private final static Logger logger = LoggerFactory.getLogger(RubusMedia.class);

	private final UUID id;

	private final String title;

	private final int duration;

	private final QueryingStrategyInterface qsi;

	/**
	 * Constructs an instance of this class.
	 * @param id the media id
	 * @param title the title
	 * @param duration the duration
	 * @param queryingStrategyInterface a querying strategy to retrieve the media content like video and audio
	 */
	public RubusMedia(UUID id, String title, int duration, QueryingStrategyInterface queryingStrategyInterface) {
		assert id != null && title != null && queryingStrategyInterface != null && duration > 0;

		this.id = id;
		this.title = title;
		this.duration = duration;
		this.qsi = queryingStrategyInterface;

		logger.debug(
			"{} instantiated, id: {}, title: {}, duration: {} QueryingStrategyInterface: {}",
			this,
			id,
			title,
			duration,
			queryingStrategyInterface
		);
	}

	@Override
	public UUID getID() {
		return id;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public SeekableByteChannel[] retrieveAudioClips(int clipIndex, int number) throws Exception {
		assert clipIndex >= 0 && number > 0 && clipIndex + number <= getDuration();

		String[] audioClipsNames = new String[number];
		for (int arrayIndex = 0; arrayIndex < audioClipsNames.length; arrayIndex++) {
			audioClipsNames[arrayIndex] = "a" + (arrayIndex + clipIndex);
		}
		return qsi.query(audioClipsNames);
	}

	@Override
	public SeekableByteChannel[] retrieveVideoClips(int clipIndex, int number) throws Exception {
		assert clipIndex >= 0 && number > 0 && clipIndex + number <= getDuration();

		String[] videoClipsNames = new String[number];
		for (int arrayIndex = 0; arrayIndex < videoClipsNames.length; arrayIndex++) {
			videoClipsNames[arrayIndex] = "v" + (arrayIndex + clipIndex);
		}
		return qsi.query(videoClipsNames);
	}

	@Override
	public MediaInfo toMediaInfo() {
		return new MediaInfo(
			getID().toString(),
			getTitle(),
			getDuration()
		);
	}

	/**
	 * Compares the RubusMedia with another object. Returns true only if the other object is an instance of {@link Media}
	 * and its fields respectively are equal to these fields.
	 * @param obj an object
	 * @return true if the other object is a Media and their fields are respectively equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Media media) {
			try {
				return
					getID().equals(media.getID()) &&
						getTitle().equals(media.getTitle()) &&
						getDuration() == media.getDuration();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return false;
	}
}
