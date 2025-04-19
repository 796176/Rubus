/*
 * Rubus is an application level protocol for video and audio streaming and
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

package backend.spring;

import backend.*;
import backend.io.MediaPool;
import common.Config;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class RubusConfiguration {

	@Bean
	@Value("${rubus.workingDir}/rubus.conf")
	Config config(Path configPath) throws IOException {
		return new Config(configPath);
	}

	@Bean
	ExecutorService requestExecutorService(Config config) {
		int maxAvailableThreads = Integer.parseInt(config.get("available-threads-limit"));
		return Executors.newFixedThreadPool(maxAvailableThreads);
	}

	@Bean
	RubusServerSocket rubusServerSocket(Config config, ExecutorService requestExecutorService) throws IOException {
		String protocol = config.get("connection-protocol");
		RubusServerSocket rubusServerSocketImpl;
		switch (protocol) {
			case "tcp" -> {
				String address = config.get("bind-address");
				int port = Integer.parseInt(config.get("listening-port"));
				rubusServerSocketImpl = new TCPRubusServerSocket(InetAddress.getByName(address), port);
			}
			default -> throw new RuntimeException("The protocol " + protocol + " is not available");
		}
		boolean secureConnectionHandshakeDisabled =
			config.get("secure-connection-handshake-disabled") != null &&
			Boolean.parseBoolean(config.get("secure-connection-handshake-disabled"));
		if (secureConnectionHandshakeDisabled) {
			return rubusServerSocketImpl;
		}
		else {
			int maxConnections = Integer.parseInt(config.get("open-connections-limit"));
			long handshakeTimeout = Long.parseLong(config.get("secure-connection-handshake-timeout"));
			return new SecureServerSocketDecorator(
				rubusServerSocketImpl, config, maxConnections, requestExecutorService, handshakeTimeout
			);
		}
	}

	@Bean
	DataSource dataSource(
		Config config,
		@Value("${rubus.db.user}") String user,
		@Value("${rubus.db.password}") String password
	) {
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setUser(user);
		dataSource.setPassword(password);
		String databaseAddress = config.get("database-address");
		dataSource.setServerNames(new String[]{ databaseAddress });
		int databasePort = Integer.parseInt(config.get("database-port"));
		dataSource.setPortNumbers(new int[] { databasePort });
		String databaseName = config.get("database-name");
		dataSource.setDatabaseName(databaseName);
		return dataSource;
	}

	@Bean
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	MediaPool mediaPool(JdbcTemplate jdbcTemplate) {
		return new MediaPool(jdbcTemplate);
	}

	@Bean
	RequestParserStrategy preProcessRequestParser() {
		return new PreProcessRequestParser();
	}

	@Primary
	@Bean
	RequestParserStrategy standardRequestParser() {
		return new StandardRequestParser();
	}

	@Bean
	SocketManager socketManager(
		MediaPool mediaPool,
		ExecutorService requestExecutorService,
		RequestParserStrategy requestParserStrategy
	) {
		return new SocketManager(mediaPool, requestExecutorService, requestParserStrategy);
	}

	@Bean
	RubusServer rubusServer(Config config, SocketManager socketManager, RubusServerSocket serverSocket) {
		int maxOpenConnections = Integer.parseInt(config.get("open-connections-limit"));
		return new RubusServer(socketManager, serverSocket, maxOpenConnections);
	}

	public static void main(String[] args) throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(RubusConfiguration.class);
		RubusServer rubusServer = applicationContext.getBean(RubusServer.class);
		rubusServer.start();
		rubusServer.join();
	}
}
