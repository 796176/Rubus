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

import common.net.response.body.PlaybackInfo;

import java.io.IOException;
import java.nio.file.Path;

/**
 * MediaProxy is an abstract class to access the media information. Only the media id is stored locally; access to
 * the rest of the information is delegated to the subject.
 * It's a proxy participant of the proxy pattern.
 */
public abstract class MediaProxy implements Media {

	private final String id;

	private Media subject = null;

	/**
	 * Invoked by the subclasses.
	 * @param mediaID the media id
	 */
	public MediaProxy(String mediaID) {
		assert mediaID != null;

		id = mediaID;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public String getTitle() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getTitle();
	}

	@Override
	public int getDuration() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getDuration();
	}

	@Override
	public int getVideoWidth() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getVideoWidth();
	}

	@Override
	public int getVideoHeight() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getVideoHeight();
	}

	@Override
	public String getVideoCodec() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getVideoCodec();
	}

	@Override
	public String getAudioCodec() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getAudioCodec();
	}

	@Override
	public String getVideoContainer() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getVideoContainer();
	}

	@Override
	public String getAudioContainer() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getAudioContainer();
	}

	@Override
	public Path getContentPath() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getContentPath();
	}

	@Override
	public byte[][] fetchAudioPieces(int pieceIndex, int number) throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.fetchAudioPieces(pieceIndex, number);
	}

	@Override
	public byte[][] fetchVideoPieces(int pieceIndex, int number) throws IOException{
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.fetchVideoPieces(pieceIndex, number);
	}

	@Override
	public PlaybackInfo toPlaybackInfo() throws IOException {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.toPlaybackInfo();
	}
}
