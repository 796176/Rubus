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

package backend.stubs;

import backend.exceptions.NotImplementedExceptions;
import backend.models.Media;
import jakarta.annotation.Nonnull;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;
import java.util.function.BiFunction;

public class MediaStub implements Media {

	public UUID id = UUID.randomUUID();

	public String title = id.toString();

	public int duration = 1;

	public URI contentUri = URI.create("");

	public BiFunction<Integer, Integer, SeekableByteChannel[]> retrieveVideoStrategy = (i1, i2) -> {
		throw new NotImplementedExceptions();
	};

	public BiFunction<Integer, Integer, SeekableByteChannel[]> retrieveAudioStrategy = (i1, i2) -> {
		throw new NotImplementedExceptions();
	};

	@Nonnull
	@Override
	public UUID getID() {
		return id;
	}

	@Nonnull
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Nonnull
	@Override
	public URI getContentURI() {
		return contentUri;
	}

	@Nonnull
	@Override
	public SeekableByteChannel[] retrieveAudioClips(int offset, int amount) {
		return retrieveAudioStrategy.apply(offset, amount);
	}

	@Nonnull
	@Override
	public SeekableByteChannel[] retrieveVideoClips(int offset, int amount) {
		return retrieveVideoStrategy.apply(offset, amount);
	}
}
