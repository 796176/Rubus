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

/**
 * PostgresMediaPool is an implementation of {@link MediaPool} that uses a Postgres database as an underlying storage.
 */
@Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
public class PostgresMediaPool implements MediaPool {

	private final static Logger logger = LoggerFactory.getLogger(PostgresMediaPool.class);

	private JdbcTemplate jdbcTemplate;

	/**
	 * Constructs an instance of this class.
	 * @param jdbcTemplate the JdbcTemplate instance that is connected to the database containing the 'media' table
	 */
	public PostgresMediaPool(JdbcTemplate jdbcTemplate) {
		assert jdbcTemplate != null;

		this.jdbcTemplate = jdbcTemplate;
		logger.debug("{} instantiated, JdbcTemplate: {}", this, jdbcTemplate);
	}

	@Override
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
	 * Returns an array containing media whose title matches the specified search query.<br>
	 * The syntax of the search query is described <a href="https://www.postgresql.org/docs/current/textsearch-controls.html#TEXTSEARCH-PARSING-QUERIES>here</a>
	 * in the context of the websearch_to_tsquery function.
	 * @param searchQuery the search query
	 * @return an array containing media whose title matches the specified search query
	 */
	@Override
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

	@Override
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

	@Override
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	@Override
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
