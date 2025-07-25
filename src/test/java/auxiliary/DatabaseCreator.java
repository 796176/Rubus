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

import common.Config;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.*;
import java.util.HexFormat;

public class DatabaseCreator {
	public static EmbeddedDatabase createdMediaFilledDB() throws SQLException {
		Path media1ContentPath =
			Path.of(System.getProperty("user.dir"), "src", "test", "resources", "data1");
		Path media2ContentPath =
			Path.of(System.getProperty("user.dir"), "src", "test", "resources", "data2");
		EmbeddedDatabase dataSource = new EmbeddedDatabaseBuilder()
			.generateUniqueName(true)
			.setType(EmbeddedDatabaseType.HSQL)
			.addScript("hsql_media_init.sql")
			.build();
		try (
			Connection connection = dataSource.getConnection();
			PreparedStatement ps = connection.prepareStatement("update media set media_content_uri = ? where id=?;")
		) {
			ps.setString(1, media1ContentPath.toString());
			ps.setString(2, "00000000-0000-4000-b000-000000000000");
			ps.execute();

			ps.setString(1, media2ContentPath.toString());
			ps.setString(2, "11111111-1111-4111-b111-111111111111");
			ps.execute();
		}
		return dataSource;
	}

	public static ApplicationContext wrapDS(DataSource ds) {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		beanFactory.registerSingleton("dataSource", ds);
		applicationContext.refresh();
		return applicationContext;
	}
}
