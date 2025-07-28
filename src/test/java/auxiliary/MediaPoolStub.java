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
import backend.io.MediaPool;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class MediaPoolStub implements MediaPool {

	public Media[] availableMedia;

	public Function<String, Media[]> searchStrategy;

	public MediaPoolStub(Media[] availableMedia, Function<String, Media[]> searchStrategy) {
		this.availableMedia = availableMedia;
		this.searchStrategy = searchStrategy;
	}

	@Override
	public Media[] availableMedia() {
		return availableMedia;
	}

	@Override
	public Media[] searchMedia(String searchQuery) {
		if (searchStrategy == null) return null;
		return searchStrategy.apply(searchQuery);
	}

	@Override
	public Media getMedia(UUID mediaId) throws IOException {
		if (availableMedia == null) return null;
		for (Media m: availableMedia()) {
			if (Objects.equals(m.getID(), mediaId)) return m;
		}
		return null;
	}

	@Override
	public JdbcTemplate getJdbcTemplate() {
		return null;
	}

	@Override
	public void setJdbcTemplate(JdbcTemplate newJdbcTemplate) {

	}
}
