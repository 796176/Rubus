/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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

package backend.main;

import backend.authontication.Authenticator;
import backend.authontication.DefaultAuthenticator;
import backend.controllers.RequestProcessor;
import backend.interactors.DefaultMediaProvider;
import backend.interactors.MediaDataAccess;
import backend.interactors.MediaProvider;
import backend.persistence.PostgresAccessStrategy;
import backend.persistence.SerializableTransactionFailureAdvising;
import backend.persistence.SqlAccessStrategy;
import backend.persistence.SqlMediaDataAccess;
import backend.querying.DefaultQueryingStrategyFactory;
import backend.querying.QueryingStrategyFactory;
import backend.authorization.BasicViewerAuthorizer;
import backend.authorization.ViewerAuthorizer;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.RollbackOn;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Configuration, dependency injection, and embedded container launch.
 */
@SpringBootApplication
@EnableTransactionManagement(proxyTargetClass = true, rollbackOn = RollbackOn.ALL_EXCEPTIONS)
@ComponentScan({"backend.controllers", "backend.converters"})
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
	DataSource dataSource(
		Config config,
		@Value("${rubus.db.user}") String user,
		@Value("${rubus.db.password}") String password
	) {
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setUser(user);
		dataSource.setPassword(password);
		String databaseAddress = config.get("database-address");
		dataSource.setServerNames(new String[] {databaseAddress});
		int databasePort = Integer.parseInt(config.get("database-port"));
		dataSource.setPortNumbers(new int[] {databasePort});
		String databaseName = config.get("database-name");
		dataSource.setDatabaseName(databaseName);
		return dataSource;
	}

	@Bean
	JdbcTemplate jdbcTemplate(Config config, DataSource dataSource) {
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
	SerializableTransactionFailureAdvising serializableTransactionFailureAdvising(Config config) {
		int retryAttempts = Integer.parseInt(config.get("transaction-failure-retry-attempts"));
		return new SerializableTransactionFailureAdvising(retryAttempts);
	}

	@Bean
	ViewerAuthorizer viewerValidator() {
		return new BasicViewerAuthorizer();
	}

	@Bean
	QueryingStrategyFactory queryingStrategyFactory() {
		return new DefaultQueryingStrategyFactory();
	}

	@Bean
	SqlAccessStrategy sqlAccessStrategy(JdbcTemplate jdbcTemplate) {
		return new PostgresAccessStrategy(jdbcTemplate);
	}

	@Bean
	MediaDataAccess mediaDataAccess(
		QueryingStrategyFactory queryingStrategyFactory, SqlAccessStrategy sqlAccessStrategy
	) {
		return new SqlMediaDataAccess(queryingStrategyFactory, sqlAccessStrategy);
	}

	@Bean
	MediaProvider mediaProvider(ViewerAuthorizer viewerAuthorizer, MediaDataAccess mediaDataAccess) {
		return new DefaultMediaProvider(viewerAuthorizer, mediaDataAccess);
	}

	@Bean
	Authenticator authenticator() {
		return new DefaultAuthenticator();
	}

	@Bean
	RequestProcessor requestProcessor(MediaProvider mediaProvider, Authenticator authenticator) {
		return new RequestProcessor(mediaProvider, authenticator);
	}

	@Bean
	WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryWebServerFactoryCustomizer(
		Config config
	) throws UnknownHostException {
		boolean secureConnectionEnabled = Boolean.parseBoolean(config.get("secure-connection-enabled"));
		String certificateLocation = config.get("certificate-location");
		String privateKeyLocation = config.get("private-key-location");
		int listeningPort = Integer.parseInt(config.get("listening-port"));
		InetAddress bindAddress = InetAddress.getByName(config.get("bind-address"));
		return factory -> {
			if (privateKeyLocation != null && certificateLocation != null && secureConnectionEnabled) {
				Ssl ssl = new Ssl();
				ssl.setCertificate("file:" + certificateLocation);
				ssl.setCertificatePrivateKey("file:" + privateKeyLocation);
				factory.setSsl(ssl);
			}
			factory.setPort(listeningPort);
			factory.setAddress(bindAddress);
		};
	}

	@Bean("applicationTaskExecutor")
	AsyncTaskExecutor asyncTaskExecutor(Config config) {
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setCorePoolSize(Integer.parseInt(config.get("available-threads-limit")));
		return threadPoolTaskExecutor;
	}

	public static void main(String[] args) {
		if (logger.isInfoEnabled()) {
			logger.info("Starting process with arguments: {}", Arrays.toString(args));
		}
		SpringApplication springApplication = new SpringApplication(RubusConfiguration.class);
		springApplication.setBannerMode(Banner.Mode.OFF);
		springApplication.run(args);
	}
}
