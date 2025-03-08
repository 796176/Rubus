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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A class-container to store the information about the media.
 */
public class RubusMedia implements Media {
	private final String id;
	private final String title;
	private final int duration;
	private final int videoWidth;
	private final int videoHeight;
	private final String videoContainer;
	private final String audioContainer;
	private final String videoCodec;
	private final String audioCodec;
	private final Path contentPath;

	/**
	 * Constructs an instance of this class.
	 * @param id the media id
	 * @param title the title
	 * @param duration the duration
	 * @param videoWidth the video width in pixels
	 * @param videoHeight the video height in pixels
	 * @param videoContainer the video container
	 * @param audioContainer the audio container
	 * @param videoCodec the video codec
	 * @param audioCodec the audio codec
	 * @param contentPath the location to a directory where media pieces are stored in
	 */
	public RubusMedia(
		String id,
		String title,
		int duration,
		int videoWidth,
		int videoHeight,
		String videoContainer,
		String audioContainer,
		String videoCodec,
		String audioCodec,
		Path contentPath
	) {
		assert id != null && title != null && contentPath != null && duration > 0 && videoWidth >= 0 && videoHeight >= 0;

		this.id = id;
		this.title = title;
		this.duration = duration;
		this.videoWidth = videoWidth;
		this.videoHeight = videoHeight;
		this.videoContainer = videoContainer;
		this.audioContainer = audioContainer;
		this.videoCodec = videoCodec;
		this.audioCodec = audioCodec;
		this.contentPath = contentPath;
	}

	@Override
	public String getID() {
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
	public int getVideoWidth() {
		return videoWidth;
	}

	@Override
	public int getVideoHeight() {
		return videoHeight;
	}

	@Override
	public String getVideoCodec() {
		return videoCodec;
	}

	@Override
	public String getAudioCodec() {
		return audioCodec;
	}

	@Override
	public String getVideoContainer() {
		return videoContainer;
	}

	@Override
	public String getAudioContainer() {
		return audioContainer;
	}

	@Override
	public Path getContentPath() {
		return contentPath;
	}

	@Override
	public byte[][] fetchAudioPieces(int pieceIndex, int number) throws IOException {
		assert pieceIndex >= 0 && number > 0 && pieceIndex + number <= getDuration();

		byte[][] audioPieces = new byte[number][];
		for (int arrayIndex = 0; arrayIndex < audioPieces.length; arrayIndex++) {
			Path audioPiecePath = Path.of(contentPath.toString(), "a" + (arrayIndex + pieceIndex) + "." + getAudioContainer());
			audioPieces[arrayIndex] = Files.exists(audioPiecePath) ? Files.readAllBytes(audioPiecePath) : null;
		}
		return audioPieces;
	}

	@Override
	public byte[][] fetchVideoPieces(int pieceIndex, int number) throws IOException {
		assert pieceIndex >= 0 && number > 0 && pieceIndex + number <= getDuration();

		byte[][] videoPieces = new byte[number][];
		for (int arrayIndex = 0; arrayIndex < videoPieces.length; arrayIndex++) {
			Path videoPiecePath = Path.of(contentPath.toString(), "v" + (arrayIndex + pieceIndex) + "." + getVideoContainer());
			videoPieces[arrayIndex] = Files.exists(videoPiecePath) ? Files.readAllBytes(videoPiecePath) : null;
		}
		return videoPieces;
	}

	@Override
	public MediaInfo toMediaInfo() {
		return new MediaInfo(
			getID(),
			getTitle(),
			getVideoWidth(),
			getVideoHeight(),
			getDuration(),
			getVideoCodec(),
			getAudioCodec(),
			getVideoContainer(),
			getAudioContainer()
		);
	}

	/**
	 * Compares the RubusMedia with another object. Returns true only if the other object is an instance of RubusMedia
	 * and all its field are equal to these fields.
	 * @param obj an object
	 * @return true if the other object is a RubusMedia and has the fields, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RubusMedia rubusMedia) {
			return
				getID().equals(rubusMedia.getID()) &&
				getTitle().equals(rubusMedia.getTitle()) &&
				getDuration() == rubusMedia.getDuration() &&
				getVideoWidth() == rubusMedia.getVideoWidth() &&
				getVideoHeight() == rubusMedia.getVideoHeight() &&
				getVideoCodec().equals(rubusMedia.getVideoCodec()) &&
				getAudioCodec().equals(rubusMedia.getAudioCodec()) &&
				getVideoContainer().equals(rubusMedia.getVideoContainer()) &&
				getAudioContainer().equals(rubusMedia.getAudioContainer()) &&
				getContentPath().equals(rubusMedia.getContentPath());
		}
		return false;
	}
}
