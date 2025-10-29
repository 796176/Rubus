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

package frontend.spring;

import frontend.configuration.Config;
import frontend.controllers.WatchHistoryController;
import frontend.interactors.WatchHistory;
import frontend.decoders.FfmpegJniVideoDecoder;
import frontend.decoders.VideoDecoder;
import frontend.gui.MainFrame;
import frontend.gui.settings.*;
import frontend.network.HttpRubusClient;
import frontend.network.RubusClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

@Configuration
@Import(SettingsTabsConfiguration.class)
public class RubusConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(RubusConfiguration.class);

	@Bean
	@Value("${rubus.workingDir}/rubus.conf")
	Config config(Path configPath) throws IOException {
		try {
			return new Config(configPath);
		} catch (Exception e) {
			logger.error("Exception occurred while parsing config file located at {}", configPath, e);
			throw e;
		}
	}

	@Bean
	@Scope("prototype")
	RubusClient rubusClient(Config config) {
		return config.action(c -> {
			String address = c.get("bind-address");
			int port = Integer.parseInt(c.get("listening-port"));
			HttpRubusClient httpRubusClient = new HttpRubusClient(address, port);
			boolean secureConnectionEnabled = Boolean.parseBoolean(c.get("secure-connection-enabled"));
			httpRubusClient.setSecureConnectionEnabled(secureConnectionEnabled);
			boolean secureConnectionRequired = Boolean.parseBoolean(c.get("secure-connection-required"));
			httpRubusClient.setSecureConnectionRequired(secureConnectionRequired);
			return httpRubusClient;
		});
	}

	@Bean
	Object lookAndFeel(Config config) throws UnsupportedLookAndFeelException, ReflectiveOperationException {
		String laf = config.get("look-and-feel");
		try {
			UIManager.setLookAndFeel(laf);
		} catch (Exception e) {
			logger.error("Look-and-feel {} was not set", laf, e);
			throw e;
		}
		return null;
	}

	@Bean(destroyMethod = "close")
	@Value("${rubus.workingDir}/watch_history")
	WatchHistory watchHistory(Path watchHistoryPath) throws IOException {
		try {
			return new WatchHistoryController(watchHistoryPath);
		} catch (Exception e) {
			logger.error("WatchHistory failed to instantiate, history path: {}", watchHistoryPath, e);
			throw e;
		}
	}

	@Bean(destroyMethod = "close")
	VideoDecoder videoDecoder() {
		System.loadLibrary("rubus");
		return new FfmpegJniVideoDecoder();
	}

	@Bean(initMethod = "display")
	@DependsOn("lookAndFeel")
	MainFrame mainFrame(Config config, WatchHistory watchHistory, BeanFactory beanFactory, VideoDecoder videoDecoder) {
		int x = Integer.parseInt(config.get("main-frame-x"));
		int y = Integer.parseInt(config.get("main-frame-y"));
		int width = Integer.parseInt(config.get("main-frame-width"));
		int height = Integer.parseInt(config.get("main-frame-height"));
		Supplier<RubusClient> rubusClientSupplier = () -> beanFactory.getBeanProvider(RubusClient.class).getObject();
		Supplier<SettingsTabs> settingsTabsSupplier = () -> beanFactory.getBeanProvider(SettingsTabs.class).getObject();
		MainFrame mainFrame =
			new MainFrame(config, rubusClientSupplier, watchHistory, settingsTabsSupplier, videoDecoder);
		mainFrame.setBounds(x, y, width, height);
		return mainFrame;
	}

	public static void main(String[] args) {
		logger.info("Instantiating ApplicationContext");
		var applicationContext = new AnnotationConfigApplicationContext(RubusConfiguration.class);
		logger.info("ApplicationContext {} instantiated", applicationContext);
		applicationContext.registerShutdownHook();
	}
}
