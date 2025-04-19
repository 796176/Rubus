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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * A class-container to store the information about the media.
 */
public class RubusMedia implements Media {

	private final static Logger logger = LoggerFactory.getLogger(RubusMedia.class);

	private final byte[] id;
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
	 * @param videoWidth the video width in pixels
	 * @param videoHeight the video height in pixels
	 * @param duration the duration
	 * @param videoContainer the video container
	 * @param audioContainer the audio container
	 * @param videoEncoding the video codec
	 * @param audioEncoding the audio codec
	 * @param contentPath the location to a directory where media pieces are stored in
	 */
	public RubusMedia(
		byte[] id,
		String title,
		int videoWidth,
		int videoHeight,
		int duration,
		String videoEncoding,
		String audioEncoding,
		String videoContainer,
		String audioContainer,
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
		this.videoCodec = videoEncoding;
		this.audioCodec = audioEncoding;
		this.contentPath = contentPath;

		if (logger.isEnabledForLevel(Level.DEBUG)) {
			logger.debug(
				"{} was initialized, id: {}, title: {}, video width: {}, video height: {}, duration: {}" +
				"video encoding: {}, audio encoding: {}, video container: {}, audio container: {}, content path: {}",
				this,
				Arrays.toString(id),
				title,
				videoWidth,
				videoHeight,
				duration,
				videoEncoding,
				audioEncoding,
				videoContainer,
				audioContainer,
				contentPath
			);
		}
	}

	@Override
	public byte[] getID() {
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
			HexFormat.of().formatHex(getID()),
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
	 * Compares the RubusMedia with another object. Returns true only if the other object is an instance of {@link Media}
	 * and all its field are equal to these fields.
	 * @param obj an object
	 * @return true if the other object is a Media and has the fields, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Media media) {
			try {
				return
					Arrays.equals(getID(), media.getID()) &&
						getTitle().equals(media.getTitle()) &&
						getDuration() == media.getDuration() &&
						getVideoWidth() == media.getVideoWidth() &&
						getVideoHeight() == media.getVideoHeight() &&
						getVideoCodec().equals(media.getVideoCodec()) &&
						getAudioCodec().equals(media.getAudioCodec()) &&
						getVideoContainer().equals(media.getVideoContainer()) &&
						getAudioContainer().equals(media.getAudioContainer()) &&
						getContentPath().equals(media.getContentPath());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return false;
	}
}
