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

package auxiliary;

import backend.io.Media;
import common.net.response.body.MediaInfo;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

public class MediaStub implements Media {

	public UUID id;

	public String title;

	public int duration;

	public Path contentPath;

	public BiFunction<Integer, Integer, byte[][]> videoFetchingStrategy;

	public BiFunction<Integer, Integer, byte[][]> audioFetchingStrategy;

	public MediaStub(
		UUID id,
		String title,
		int duration,
		Path contentPath,
		BiFunction<Integer, Integer, byte[][]> videoFetchingStrategy,
		BiFunction<Integer, Integer, byte[][]> audioFetchingStrategy
	) {
		this.id = id;
		this.title = title;
		this.duration = duration;
		this.contentPath = contentPath;
		this.videoFetchingStrategy = videoFetchingStrategy;
		this.audioFetchingStrategy = audioFetchingStrategy;
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
	public Path getContentPath() {
		return contentPath;
	}

	@Override
	public byte[][] fetchAudioPieces(int clipOffset, int number) {
		if (audioFetchingStrategy == null) return null;
		return audioFetchingStrategy.apply(clipOffset, number);
	}

	@Override
	public byte[][] fetchVideoPieces(int clipOffset, int number) {
		if (videoFetchingStrategy == null) return null;
		return videoFetchingStrategy.apply(clipOffset, number);
	}

	@Override
	public MediaInfo toMediaInfo() {
		return new MediaInfo(Objects.toString(getID()), getTitle(), getDuration());
	}
}
