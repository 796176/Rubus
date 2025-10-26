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
import backend.exceptions.CommonDataAccessException;
import backend.interactors.MediaDataAccess;
import backend.models.Media;
import jakarta.annotation.Nonnull;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.Function;

public class MediaDataAccessStub implements MediaDataAccess {

	public Supplier<Media[]> getMultipleMediaSupplier = () -> { throw new NotImplementedExceptions(); };

	public Function<UUID, Media> getSingleMediaFunction = media -> { throw new NotImplementedExceptions(); };

	public Function<String, Media[]> searchMediaFunction = query -> { throw new NotImplementedExceptions(); };

	@Nonnull
	@Override
	public Media[] getMedia() throws CommonDataAccessException {
		return getMultipleMediaSupplier.get();
	}

	@Override
	public Media getMedia(@Nonnull UUID mediaId) throws CommonDataAccessException {
		return getSingleMediaFunction.apply(mediaId);
	}

	@Nonnull
	@Override
	public Media[] searchMedia(@Nonnull String searchQuery) throws CommonDataAccessException {
		return searchMediaFunction.apply(searchQuery);
	}
}
