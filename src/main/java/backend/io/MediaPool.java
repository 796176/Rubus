/*
 * Rubus is an application layer protocol for video and audio streaming and
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The client uses MediaPool to access the information about the available media.
 */
public class MediaPool {

	private final static Logger logger = LoggerFactory.getLogger(MediaPool.class);

	private JdbcTemplate jdbcTemplate;

	private AtomicBoolean cacheUpdateNeeded = new AtomicBoolean(true);

	private Media[] cachedMedia = new Media[0];

	/**
	 * Constructs an instance of this class.
	 * @param jdbcTemplate the JdbcTemplate instance that is connected to the database containing the 'media' table
	 */
	public MediaPool(JdbcTemplate jdbcTemplate) {
		assert jdbcTemplate != null;

		this.jdbcTemplate = jdbcTemplate;
		logger.debug("{} instantiated, JdbcTemplate: {}", this, jdbcTemplate);
	}

	/**
	 * Returns an array containing all the available media represented as {@link Media}.
	 * @return an array containing all the available media represented as {@link Media}
	 * @throws IOException if some I/O error occurs
	 */
	public Media[] availableMedia() throws IOException {
		String sqlQuery = "select * from media;";
		logger.debug("{} querying {}", this, sqlQuery);
		return jdbcTemplate.query(sqlQuery, rs -> {
			ArrayList<Media> media = new ArrayList<>();
			while (rs.next()) {
				media.add(
					new RubusMedia(
						rs.getBytes("id"),
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
	 * Same as {@link #availableMedia} but it doesn't retrieve all the data in a single operation; the rest of the data
	 * is retrieved only when accessed. Invoking this method may be preferential when only partial data access is
	 * performed.
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
			logger.debug("{} querying {}", this, sqlQuery);
			cachedMedia = jdbcTemplate.query(sqlQuery, rs -> {
				ArrayList<Media> media = new ArrayList<>();
				while (rs.next()) {
					media.add(
						new TitledMediaProxy(
							this,
							rs.getBytes("id"),
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
	 * Returns a {@link Media} instance associated with the specified id.
	 * @param mediaId the media id
	 * @return the {@link Media} instance
	 * @throws IOException if some I/O occurs
	 */
	@Transactional()
	public Media getMedia(byte[] mediaId) throws IOException {
		String sql = "select * from media where id=?;";
		logger.debug("{} querying {}", this, sql);
		return jdbcTemplate.query(
			sql,
			preparedStatement -> {
				preparedStatement.setBytes(1, mediaId);
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
