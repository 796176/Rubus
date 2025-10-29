/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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

import backend.exceptions.CommonSecurityException;
import backend.exceptions.NotImplementedExceptions;
import backend.interactors.MediaProvider;
import backend.models.Media;
import backend.models.Viewer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MediaProviderStub implements MediaProvider {

	public BiFunction<Viewer, UUID, Media> getSingleMediaStrategy = (viewer, id) -> {
		throw new NotImplementedExceptions();
	};

	public Function<Viewer, Media[]> getMultipleMediaStrategy = viewer -> {
		throw new NotImplementedExceptions();
	};

	public BiFunction<Viewer, String, Media[]> searchMedia = (viewer, query) -> {
		throw new NotImplementedExceptions();
	};

	@Nullable
	@Override
	public Media getMedia(@Nonnull Viewer viewer, @Nonnull UUID mediaId) throws CommonSecurityException {
		return getSingleMediaStrategy.apply(viewer, mediaId);
	}

	@Nonnull
	@Override
	public Media[] getMedia(@Nonnull Viewer viewer) throws CommonSecurityException {
		return getMultipleMediaStrategy.apply(viewer);
	}

	@Nonnull
	@Override
	public Media[] searchMedia(@Nonnull Viewer viewer, @Nonnull String searchQuery) throws CommonSecurityException {
		return searchMedia.apply(viewer, searchQuery);
	}
}
