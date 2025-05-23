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

package frontend.spring;

import common.Config;
import common.RubusSocket;
import common.TCPRubusSocket;
import common.ssl.HandshakeFailedException;
import common.ssl.SecureSocket;
import frontend.WatchHistory;
import frontend.gui.MainFrame;
import frontend.gui.settings.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.function.Supplier;

@Configuration
@Import(SettingsTabsConfiguration.class)
public class RubusConfiguration {

	@Bean
	@Value("${rubus.workingDir}/rubus.conf")
	Config config(Path configPath) throws IOException {
		return new Config(configPath);
	}

	@Bean
	@Scope("prototype")
	RubusSocket socket(Config config) throws Exception {
		return config.action(c -> {
			String protocol = c.get("connection-protocol");
			RubusSocket socket;
			switch (protocol) {
				case "tcp" -> {
					InetAddress address = InetAddress.getByName(c.get("bind-address"));
					int port = Integer.parseInt(c.get("listening-port"));
					socket = new TCPRubusSocket(address, port);
				}
				default -> {
					throw new RuntimeException("The " + protocol + "transport layer protocol is not available");
				}
			}
			boolean secureConnectionHandshakeDisabled =
				c.get("secure-connection-handshake-disabled") != null &&
				Boolean.parseBoolean(c.get("secure-connection-handshake-disabled"));
			if (secureConnectionHandshakeDisabled) {
				return socket;
			} else {
				boolean secureConnectionRequired = Boolean.parseBoolean(c.get("secure-connection-required"));
				long handshakeTimeout = Long.parseLong(c.get("secure-connection-handshake-timeout"));
				try {
					return new SecureSocket(socket, c, handshakeTimeout, true);
				} catch (HandshakeFailedException e) {
					if (!secureConnectionRequired) return socket;
					else throw e;
				}
			}
		});
	}

	@Bean
	Object lookAndFeel(Config config) throws UnsupportedLookAndFeelException, ReflectiveOperationException {
		UIManager.setLookAndFeel(config.get("look-and-feel"));
		return null;
	}

	@Bean(destroyMethod = "close")
	@Value("${rubus.workingDir}/watch_history")
	WatchHistory watchHistory(Path watchHistoryPath) throws IOException {
		return new WatchHistory(watchHistoryPath);
	}

	@Bean(initMethod = "display")
	@DependsOn("lookAndFeel")
	MainFrame mainFrame(Config config, WatchHistory watchHistory, BeanFactory beanFactory) {
		int x = Integer.parseInt(config.get("main-frame-x"));
		int y = Integer.parseInt(config.get("main-frame-y"));
		int width = Integer.parseInt(config.get("main-frame-width"));
		int height = Integer.parseInt(config.get("main-frame-height"));
		Supplier<RubusSocket> rubusSocketSupplier = () -> beanFactory.getBeanProvider(RubusSocket.class).getObject();
		Supplier<SettingsTabs> settingsTabsSupplier = () -> beanFactory.getBeanProvider(SettingsTabs.class).getObject();
		MainFrame mainFrame = new MainFrame(config, rubusSocketSupplier, watchHistory, settingsTabsSupplier);
		mainFrame.setBounds(x, y, width, height);
		return mainFrame;
	}

	public static void main(String[] args) {
		var applicationContext = new AnnotationConfigApplicationContext(RubusConfiguration.class);
		applicationContext.registerShutdownHook();
	}
}
