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

package backend.converters;

import backend.models.MediaList;

import java.util.Map;
import java.util.UUID;

public class MediaListBinaryConverterTests extends BinaryConverterTests<MediaList> {

	@Override
	public MediaList getModel() {
		return new MediaList(Map.of(
			UUID.fromString("3281e88b-15ee-4f5c-91c7-3d77b5ba1d0a"), "title1",
			UUID.fromString("a6b019d2-b3af-4780-86d8-0ddd7b835cd3"), "title2"
		));
	}

	@Override
	public BinaryConverter<MediaList> getConverter() {
		return new MediaListBinaryConverter();
	}

	@Override
	public boolean testEquality(MediaList m1, MediaList m2) {
		return m1.media().equals(m2.media());
	}
}
