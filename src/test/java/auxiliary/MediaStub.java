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
import backend.querying.QueryingStrategyFactory;
import common.net.response.body.MediaInfo;

import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

public class MediaStub implements Media {

	public UUID id;

	public String title;

	public int duration;

	public BiFunction<Integer, Integer, SeekableByteChannel[]> videoFetchingStrategy;

	public BiFunction<Integer, Integer, SeekableByteChannel[]> audioFetchingStrategy;

	public MediaStub(
		UUID id,
		String title,
		int duration,
		BiFunction<Integer, Integer, SeekableByteChannel[]> videoFetchingStrategy,
		BiFunction<Integer, Integer, SeekableByteChannel[]> audioFetchingStrategy
	) {
		this.id = id;
		this.title = title;
		this.duration = duration;
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
	public SeekableByteChannel[] retrieveAudioClips(int clipIndex, int number) {
		if (audioFetchingStrategy == null) return null;
		return audioFetchingStrategy.apply(clipIndex, number);
	}

	@Override
	public SeekableByteChannel[] retrieveVideoClips(int clipIndex, int number) {
		if (videoFetchingStrategy == null) return null;
		return videoFetchingStrategy.apply(clipIndex, number);
	}

	@Override
	public MediaInfo toMediaInfo() {
		return new MediaInfo(Objects.toString(getID()), getTitle(), getDuration());
	}
}
