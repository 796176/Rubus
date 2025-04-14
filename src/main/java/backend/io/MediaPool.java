/*
 * Rubus is an application level protocol for video and audio streaming and
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

import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MediaPool class allows the client to query information about the available media. It encapsulates the internal logic
 * of how MediaPool communicates with the underlying source of information ( e.g. a database ), which allows the client
 * to just use it without knowing how it works under the hood.
 */
public class MediaPool {

	private JdbcTemplate jdbcTemplate;

	private AtomicBoolean cacheUpdateNeeded = new AtomicBoolean(true);

	private Media[] cachedMedia = new Media[0];

	/**
	 * Constructs an instance of this class with the database storing the essential information. The database must
	 * have the media table containing specific column names and their types.<br>
	 * {@link JdbcTemplate} is a class provided by the Spring Framework, and it's used here to streamline the database
	 * access as it provides {@link java.sql.SQLException} wrapping, thread safety, and automatic connection closing.
	 * @param jdbcTemplate a JdbcTemplate instance
	 */
	public MediaPool(JdbcTemplate jdbcTemplate) {
		assert jdbcTemplate != null;

		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Returns an array containing all the available media represented as {@link Media}.
	 * @return an array containing all the available media represented as {@link Media}
	 * @throws IOException if some I/O error occurs
	 */
	public Media[] availableMedia() throws IOException {
		String sqlQuery = "select * from media;";
		return jdbcTemplate.query(sqlQuery, rs -> {
			ArrayList<Media> media = new ArrayList<>();
			while (rs.next()) {
				media.add(
					new RubusMedia(
						HexFormat.of().formatHex(rs.getBytes("id")),
						new String(rs.getBytes("title")),
						rs.getInt("videoWidth"),
						rs.getInt("videoHeight"),
						rs.getInt("duration"),
						new String(rs.getBytes("videoEncoding")),
						new String(rs.getBytes("audioEncoding")),
						new String(rs.getBytes("videoContainer")),
						new String(rs.getBytes("audioContainer")),
						Path.of(new String(rs.getBytes("contentPath")))
					)
				);
			}
			return media.toArray(new Media[0]);
		});
	}

	/**
	 * Same as {@link #availableMedia} but potentially performs the retrieval faster because it's not necessary refer
	 * to the database.
	 * @return an array containing all the available media represented as {@link Media}
	 * @throws IOException if some I/O error occurs
	 */
	public Media[] availableMediaFast() throws IOException {
		if (!cacheUpdateNeeded.get()) return cachedMedia;
		synchronized (this) {
			// a condition statement for threads that had been locked meaning cacheUpdateNeeded could've been flipped by
			// a preceding thread
			if (!cacheUpdateNeeded.get()) return cachedMedia;
			String sqlQuery = "select id, title from media;";
			cachedMedia = jdbcTemplate.query(sqlQuery, rs -> {
				ArrayList<Media> media = new ArrayList<>();
				while (rs.next()) {
					media.add(
						new TitledMediaProxy(
							this,
							HexFormat.of().formatHex(rs.getBytes("id")),
							new String(rs.getBytes("title"))
						)
					);
				}
				return media.toArray(new Media[0]);
			});
			cacheUpdateNeeded.set(false);
			return cachedMedia;
		}
	}

	/**
	 * Returns the media represented as {@link Media} with the specified media id.
	 * @param mediaId the id of the media
	 * @return the {@link Media} object
	 * @throws IOException if some I/O occurs
	 */
	public Media getMedia(String mediaId) throws IOException {
		byte[] id = HexFormat.of().parseHex(mediaId);
		String sql = "select * from media where id=?;";
		return jdbcTemplate.query(
			sql,
			preparedStatement -> {
				preparedStatement.setBytes(1, id);
			},
			rs -> {
				if (!rs.next()) return null;
				return new RubusMedia(
					mediaId,
					new String(rs.getBytes("title")),
					rs.getInt("videoWidth"),
					rs.getInt("videoHeight"),
					rs.getInt("duration"),
					new String(rs.getBytes("videoEncoding")),
					new String(rs.getBytes("audioEncoding")),
					new String(rs.getBytes("videoContainer")),
					new String(rs.getBytes("audioContainer")),
					Path.of(new String(rs.getBytes("contentPath")))
				);
			}
		);
	}

	/**
	 * Returns the current JdbcTemplate instance.
	 * @return the current JdbcTemplate instance
	 */
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	/**
	 * Sets a new JdbcTemplate instance.
	 * @param newJdbcTemplate a new JdbcTemplate instance
	 */
	public void setJdbcTemplate(JdbcTemplate newJdbcTemplate) {
		assert newJdbcTemplate != null;

		jdbcTemplate = newJdbcTemplate;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MediaPool mediaPool) {
			return getJdbcTemplate().equals(mediaPool.getJdbcTemplate());
		}
		return false;
	}
}
