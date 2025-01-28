/*
 * Rubus is an application level protocol for video and audio streaming and
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

package backend.io;

public abstract class MediaProxy implements Media {

	private final String id;

	private Media subject = null;

	public MediaProxy(String mediaID) {
		assert mediaID != null;

		id = mediaID;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public String getTitle() {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getTitle();
	}

	@Override
	public long getTotalPieces() {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getTotalPieces();
	}

	@Override
	public double getPieceDuration() {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getPieceDuration();
	}

	@Override
	public String getAudioEncodingType() {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getAudioEncodingType();
	}

	@Override
	public String getVideoEncodingType() {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.getVideoEncodingType();
	}

	@Override
	public byte[][] fetchAudioPieces(long pieceIndex, int number) {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.fetchAudioPieces(pieceIndex, number);
	}

	@Override
	public byte[][] fetchVideoPieces(long pieceIndex, int number) {
		if (subject == null) subject = MediaPool.getMedia(getID());
		return subject.fetchVideoPieces(pieceIndex, number);
	}
}
