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

package backend.spring;

import backend.*;
import backend.io.MediaPool;
import backend.io.TransactionLockFailureAdvising;
import common.Config;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.RollbackOn;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAspectJAutoProxy
@EnableTransactionManagement(proxyTargetClass = true, rollbackOn = RollbackOn.ALL_EXCEPTIONS)
public class RubusConfiguration {

	private final static Logger logger = LoggerFactory.getLogger(RubusConfiguration.class);

	@Bean
	@Value("${rubus.workingDir}/rubus.conf")
	Config config(Path configPath) throws IOException {
		try {
			return new Config(configPath).immutableConfig();
		} catch (Exception e) {
			logger.error("Exception occurred while parsing config file located at {}", configPath, e);
			throw e;
		}
	}

	@Bean
	ExecutorService requestExecutorService(Config config) {
		int maxAvailableThreads = Integer.parseInt(config.get("available-threads-limit"));
		return Executors.newFixedThreadPool(maxAvailableThreads);
	}

	@Bean
	RubusServerSocket rubusServerSocket(Config config, ExecutorService requestExecutorService) throws IOException {
		try {
			String protocol = config.get("connection-protocol");
			RubusServerSocket rubusServerSocketImpl;
			switch (protocol) {
				case "tcp" -> {
					String address = config.get("bind-address");
					int port = Integer.parseInt(config.get("listening-port"));
					rubusServerSocketImpl = new TCPRubusServerSocket(InetAddress.getByName(address), port);
				}
				default -> {
					throw new RuntimeException("The " + protocol + " protocol is not supported");
				}
			}
			boolean secureConnectionHandshakeDisabled =
				config.get("secure-connection-handshake-disabled") != null &&
					Boolean.parseBoolean(config.get("secure-connection-handshake-disabled"));
			if (secureConnectionHandshakeDisabled) {
				return rubusServerSocketImpl;
			} else {
				int maxConnections = Integer.parseInt(config.get("open-connections-limit"));
				long handshakeTimeout = Long.parseLong(config.get("secure-connection-handshake-timeout"));
				return new SecureServerSocketDecorator(
					rubusServerSocketImpl, config, maxConnections, requestExecutorService, handshakeTimeout
				);
			}
		} catch (Exception e) {
			logger.error("Failed to instantiate server socket", e);
			throw e;
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
	JdbcTemplate jdbcTemplate(DataSource dataSource, Config config) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		int timeout = Integer.parseInt(config.get("transaction-timeout"));
		jdbcTemplate.setQueryTimeout(timeout);
		return jdbcTemplate;
	}

	@Order(200)
	@Bean
	PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
		return new JdbcTransactionManager(dataSource);
	}

	@Order(1)
	@Bean
	TransactionLockFailureAdvising transactionLockFailureAdvising(Config config) {
		int retryAttempts = Integer.parseInt(config.get("transaction-failure-retry-attempts"));
		return new TransactionLockFailureAdvising(retryAttempts);
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

	@Bean(initMethod = "start", destroyMethod = "close")
	RubusServer rubusServer(Config config, SocketManager socketManager, RubusServerSocket serverSocket) {
		int maxOpenConnections = Integer.parseInt(config.get("open-connections-limit"));
		return new RubusServer(socketManager, serverSocket, maxOpenConnections);
	}

	public static void main(String[] args) {
		logger.info("Instantiating Application context");
		var applicationContext = new AnnotationConfigApplicationContext(RubusConfiguration.class);
		logger.info("ApplicationContext {} instantiated, Class: {}", applicationContext, RubusConfiguration.class);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("Shutting down ApplicationContext {}", applicationContext);
			applicationContext.close();
			logger.info("Done");
		}));
	}
}
