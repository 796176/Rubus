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

package backend.io;

import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.UUID;

/**
 * The client uses MediaPool to query the information about the available media.
 */
public interface MediaPool {
	/**
	 * Returns an array containing all the available media represented as {@link Media}.
	 * @return an array containing all the available media represented as {@link Media}
	 * @throws IOException if some I/O error occurs
	 */
	Media[] availableMedia() throws IOException;

	/**
	 * Same as {@link #availableMedia} but it doesn't retrieve all the data in a single operation; the rest of the data
	 * is retrieved only when accessed. Invoking this method may be preferential when only partial data access is
	 * performed.
	 * @return an array containing all the available media represented as {@link Media}
	 * @throws IOException if some I/O error occurs
	 */
	Media[] availableMediaFast() throws IOException;

	/**
	 * Returns an array containing media whose title matches the specified search query.<br>
	 * The syntax of the search query is determined by the concrete implementations.
	 * @param searchQuery the search query
	 * @return an array containing media whose title matches the specified search query
	 */
	Media[] searchMedia(String searchQuery);

	/**
	 * Returns a {@link Media} instance associated with the specified id.
	 * @param mediaId the media id
	 * @return the {@link Media} instance
	 * @throws IOException if some I/O occurs
	 */
	Media getMedia(UUID mediaId) throws IOException;

	/**
	 * Returns the current JdbcTemplate instance.
	 * @return the current JdbcTemplate instance
	 */
	JdbcTemplate getJdbcTemplate();

	/**
	 * Sets a new JdbcTemplate instance.
	 * @param newJdbcTemplate a new JdbcTemplate instance
	 */
	void setJdbcTemplate(JdbcTemplate newJdbcTemplate);
}
