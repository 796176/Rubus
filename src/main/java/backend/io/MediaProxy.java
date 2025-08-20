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

import common.net.response.body.MediaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;

/**
 * MediaProxy is an abstract class that stores only the {@link MediaPool} instance and the media id. Accessing other
 * media-specific information will be delegated to the subject.<br>
 * It's the proxy participant of the proxy pattern.
 */
public abstract class MediaProxy implements Media {

	private final static Logger logger = LoggerFactory.getLogger(MediaProxy.class);

	private final MediaPool mediaPool;

	private final UUID id;

	private Media subject = null;

	/**
	 * Constructs an instance of this class
	 * @param mediaPool the mediaPool
	 * @param mediaID the media id
	 */
	public MediaProxy(MediaPool mediaPool, UUID mediaID) {
		assert mediaID != null;

		this.mediaPool = mediaPool;
		id = mediaID;
		logger.debug("{} instantiated, MediaPool: {}, id: {}", this, mediaPool, mediaID);
	}
	
	public MediaPool getMediaPool() {
		return mediaPool;
	}

	@Override
	public UUID getID() {
		return id;
	}

	@Override
	public String getTitle() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getTitle();
	}

	@Override
	public int getDuration() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getDuration();
	}

	@Override
	public SeekableByteChannel[] retrieveAudioClips(int clipIndex, int number) throws Exception {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.retrieveAudioClips(clipIndex, number);
	}

	@Override
	public SeekableByteChannel[] retrieveVideoClips(int clipIndex, int number) throws Exception{
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.retrieveVideoClips(clipIndex, number);
	}

	@Override
	public MediaInfo toMediaInfo() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.toMediaInfo();
	}

	/**
	 * Compares this MediaProxy with another object. Returns true only if the other object is an instance of {@link Media}
	 * and its fields are respectively equal to these fields.
	 * @param obj an object
	 * @return true if the object is a Media and their fields are respectively equal to these fields, false otherwise
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
