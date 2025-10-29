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

package frontend.stubs;

import frontend.exceptions.NotImplementedExceptions;
import frontend.models.MediaFetch;
import frontend.models.MediaInfo;
import frontend.models.MediaList;
import frontend.network.RubusResponse;
import frontend.network.RubusResponseType;

import java.util.function.Supplier;

public class RubusResponseStub implements RubusResponse {

	public Supplier<RubusResponseType> getResponseTypeSupplier = () -> RubusResponseType.OK;

	public Supplier<MediaList> listSupplier = () -> { throw new NotImplementedExceptions(); };

	public Supplier<MediaInfo> infoSupplier = () -> { throw new NotImplementedExceptions(); };

	public Supplier<MediaFetch> fetchSupplier = () -> { throw new NotImplementedExceptions(); };

	@Override
	public RubusResponseType getResponseType() {
		return getResponseTypeSupplier.get();
	}

	@Override
	public MediaList LIST() {
		return listSupplier.get();
	}

	@Override
	public MediaInfo INFO() {
		return infoSupplier.get();
	}

	@Override
	public MediaFetch FETCH() {
		return fetchSupplier.get();
	}
}
