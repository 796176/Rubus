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
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * RubusMedia is a concrete implementation of {@link Media} that stores media-specific information.
 */
public class RubusMedia implements Media {

	private final static Logger logger = LoggerFactory.getLogger(RubusMedia.class);

	private final byte[] id;
	private final String title;
	private final int duration;
	private final Path contentPath;

	/**
	 * Constructs an instance of this class.
	 * @param id the media id
	 * @param title the title
	 * @param duration the duration
	 * @param contentPath the directory that contains this media-specific files
	 */
	public RubusMedia(
		byte[] id,
		String title,
		int duration,
		Path contentPath
	) {
		assert id != null && title != null && contentPath != null && duration > 0;

		this.id = id;
		this.title = title;
		this.duration = duration;
		this.contentPath = contentPath;

		if (logger.isDebugEnabled()) {
			logger.debug(
				"{} instantiated, id: {}, title: {}, duration: {} content path: {}",
				this,
				Arrays.toString(id),
				title,
				duration,
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
	public Path getContentPath() {
		return contentPath;
	}

	@Override
	public byte[][] fetchAudioPieces(int pieceIndex, int number) throws IOException {
		assert pieceIndex >= 0 && number > 0 && pieceIndex + number <= getDuration();

		byte[][] audioPieces = new byte[number][];
		for (int arrayIndex = 0; arrayIndex < audioPieces.length; arrayIndex++) {
			Path audioPiecePath = Path.of(contentPath.toString(), "a" + (arrayIndex + pieceIndex));
			audioPieces[arrayIndex] = Files.exists(audioPiecePath) ? Files.readAllBytes(audioPiecePath) : null;
		}
		return audioPieces;
	}

	@Override
	public byte[][] fetchVideoPieces(int pieceIndex, int number) throws IOException {
		assert pieceIndex >= 0 && number > 0 && pieceIndex + number <= getDuration();

		byte[][] videoPieces = new byte[number][];
		for (int arrayIndex = 0; arrayIndex < videoPieces.length; arrayIndex++) {
			Path videoPiecePath = Path.of(contentPath.toString(), "v" + (arrayIndex + pieceIndex));
			videoPieces[arrayIndex] = Files.exists(videoPiecePath) ? Files.readAllBytes(videoPiecePath) : null;
		}
		return videoPieces;
	}

	@Override
	public MediaInfo toMediaInfo() {
		return new MediaInfo(
			HexFormat.of().formatHex(getID()),
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
					Arrays.equals(getID(), media.getID()) &&
						getTitle().equals(media.getTitle()) &&
						getDuration() == media.getDuration() &&
						getContentPath().equals(media.getContentPath());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return false;
	}
}
