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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The client uses MediaPool to access the information about the available media.
 */
@Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
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
						UUID.fromString(rs.getString("id")),
						rs.getString("title"),
						rs.getInt("duration"),
						Path.of(rs.getString("media_content_uri"))
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
							UUID.fromString(rs.getString("id")),
							rs.getString("title")
						)
					);
				}
				return media.toArray(new Media[0]);
			});
			cacheUpdateNeeded.set(false);
			return cachedMedia;
		}
	}

	public Media[] searchMedia(String searchQuery) {
		assert searchQuery != null;

		String sqlQuery = """
			WITH w1 (res1) AS (SELECT websearch_to_tsquery('english', ?)), \
			w2 (res2) AS (SELECT numnode(res1) FROM w1) \
			SELECT id, title FROM media WHERE \
			((SELECT res1 FROM w1) @@ title_tsvector) OR ((SELECT res2 FROM w2) = 0);""";
		return jdbcTemplate.query(
			sqlQuery,
			preparedStatement -> {
				preparedStatement.setString(1, searchQuery);
			},
			rs -> {
				ArrayList<Media> media = new ArrayList<>();
				while (rs.next()) {
					media.add(
						new TitledMediaProxy(
							this,
							UUID.fromString(rs.getString("id")),
							rs.getString("title")
						)
					);
				}
				return media.toArray(new Media[0]);
			}
		);
	}

	/**
	 * Returns a {@link Media} instance associated with the specified id.
	 * @param mediaId the media id
	 * @return the {@link Media} instance
	 * @throws IOException if some I/O occurs
	 */
	public Media getMedia(UUID mediaId) throws IOException {
		String sql = "select * from media where id=?;";
		logger.debug("{} querying {}", this, sql);
		return jdbcTemplate.query(
			sql,
			preparedStatement -> {
				preparedStatement.setString(1, mediaId.toString());
			},
			rs -> {
				if (!rs.next()) return null;
				return new RubusMedia(
					mediaId,
					rs.getString("title"),
					rs.getInt("duration"),
					Path.of(rs.getString("media_content_uri"))
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
