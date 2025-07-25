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

import auxiliary.DatabaseCreator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class MediaPoolTests {

	static UUID media1ID = UUID.randomUUID();

	static Media media1 = new RubusMedia(
		media1ID,
		"Title1",
		1,
		Path.of(System.getProperty("user.dir"), "src", "test", "resources", "data1")
	);

	static UUID media2ID = UUID.randomUUID();

	static Media media2 = new RubusMedia(
		media2ID,
		"Title2",
		2,
		Path.of(System.getProperty("user.dir"), "src", "test", "resources", "data2")
	);

	static MediaPool mediaPool;

	static EmbeddedDatabase dataSource;

	@BeforeAll
	static void beforeAll() throws SQLException {
		dataSource = DatabaseCreator.createdMediaFilledDB();
		ApplicationContext applicationContext = DatabaseCreator.wrapDS(dataSource);
		mediaPool = new PostgresMediaPool(new JdbcTemplate(applicationContext.getBean(DataSource.class)));
	}

	@AfterAll
	static void afterAll() {
		dataSource.shutdown();
	}

	@Test
	void getAvailableMedia() throws IOException {
		Media[] media = mediaPool.availableMedia();
		assertEquals(2, media.length);

		if (media[0].getID().equals(media1.getID())) {
			assertEquals(media1, media[0], "The media don't match");
			assertEquals(media2, media[1], "The media don't match");
		} else {
			assertEquals(media1, media[1], "The media don't match");
			assertEquals(media2, media[0], "The media don't match");
		}
	}

	@Test
	void getAvailableMediaFast() throws IOException {
		Media[] media = mediaPool.availableMediaFast();
		assertEquals(2, media.length);

		if (media[0].getID().equals(media1.getID())) {
			assertEquals(media1, media[0], "The media don't match");
			assertEquals(media2, media[1], "The media don't match");
		} else {
			assertEquals(media1, media[1], "The media don't match");
			assertEquals(media2, media[2], "The media don't match");
		}
	}

	@Test
	void getMediaByID() throws IOException {
		Media media = mediaPool.getMedia(media2ID);
		assertEquals(media2, media);
	}
}
