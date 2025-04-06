/*
 * Rubus is an application level protocol for video and audio streaming and
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

import java.io.IOException;
import java.nio.file.Path;

/**
 * MediaProxy is an abstract class containing only the {@link MediaPool} instance and the media id value. Attempt to
 * access other fields or invoke data access methods will result in delegation of this task to the subject.
 * It's a proxy participant of the proxy pattern.
 */
public abstract class MediaProxy implements Media {

	private final MediaPool mediaPool;

	private final String id;

	private Media subject = null;

	/**
	 * Constructs an instance of this class
	 * @param mediaPool the mediaPool
	 * @param mediaID the media id
	 */
	public MediaProxy(MediaPool mediaPool, String mediaID) {
		assert mediaID != null;

		this.mediaPool = mediaPool;
		id = mediaID;
	}

	@Override
	public String getID() {
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
	public int getVideoWidth() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getVideoWidth();
	}

	@Override
	public int getVideoHeight() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getVideoHeight();
	}

	@Override
	public String getVideoCodec() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getVideoCodec();
	}

	@Override
	public String getAudioCodec() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getAudioCodec();
	}

	@Override
	public String getVideoContainer() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getVideoContainer();
	}

	@Override
	public String getAudioContainer() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getAudioContainer();
	}

	@Override
	public Path getContentPath() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.getContentPath();
	}

	@Override
	public byte[][] fetchAudioPieces(int pieceIndex, int number) throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.fetchAudioPieces(pieceIndex, number);
	}

	@Override
	public byte[][] fetchVideoPieces(int pieceIndex, int number) throws IOException{
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.fetchVideoPieces(pieceIndex, number);
	}

	@Override
	public MediaInfo toMediaInfo() throws IOException {
		if (subject == null) subject = mediaPool.getMedia(getID());
		return subject.toMediaInfo();
	}

	/**
	 * Compares this MediaProxy with another object. Returns true only the other object is an instance of MediaProxy and
	 * its media id is equal to this media id.
	 * @param obj an object
	 * @return true if the object is a MediaProxy and has the same id, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MediaProxy mediaProxy) {
			return getID().equals(mediaProxy.getID());
		}
		return false;
	}
}
