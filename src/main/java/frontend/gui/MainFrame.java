/*
 * Rubus is an application level protocol for video and audio streaming and
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

package frontend.gui;

import common.Config;
import common.RubusSocket;
import common.RubusSocketConstructionException;
import common.TCPRubusSocket;
import common.net.response.body.MediaInfo;
import frontend.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class MainFrame extends JFrame {
	private final InnerThread thread;
	private final GridBagLayout bagLayout = new GridBagLayout();
	private final GridBagConstraints constraints = new GridBagConstraints();

	private PlayerInterface player = null;
	private FetchController fetchController = null;
	private AudioPlayerInterface audioPlayer = null;
	private AudioPlayerController audioController = null;
	public MainFrame() {
		setLayout(bagLayout);
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 1;
		constraints.weighty = 1;

		MainFrameMenuBar menuBar = new MainFrameMenuBar();
		setJMenuBar(menuBar);

		menuBar.reloadButton().addActionListener(actionEvent -> {
			try {
				if (fetchController != null) {
					fetchController.setSocketSupplier(this::buildSocket);
					fetchController.close();
					fetchController.update(player);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(
					this,
					e.getMessage(),
					"Connection Error",
					JOptionPane.ERROR_MESSAGE
				);
			}
		});
		menuBar.openVideoItem().addActionListener(actionEvent -> {
			try {
				OpenVideoDialog openVideoDialog = new OpenVideoDialog(this, this::buildSocket);
				openVideoDialog.setVisible(true);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(
					this,
					e.getMessage(),
					"Connection Error",
					JOptionPane.ERROR_MESSAGE
				);
			}
		});

		menuBar.settingsItem().addActionListener(actionEvent -> {
			try {
				SettingsDialog settingsDialog = new SettingsDialog(this);
				settingsDialog.setVisible(true);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(
					this,
					e.getMessage(),
					"IOException",
					JOptionPane.ERROR_MESSAGE
				);
			}
		});

		menuBar.aboutItem().addActionListener(actionEvent -> {
			AboutDialog aboutDialog = new AboutDialog(this);
			aboutDialog.setVisible(true);
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
			try {
				if (fetchController != null) fetchController.close();
				if (audioPlayer != null) audioPlayer.terminate();

				Path configPath = Path.of(System.getProperty("user.home"), ".rubus", "client_config");
				Config config = new Config(configPath);
				config.set("main-frame-x", getX() + "");
				config.set("main-frame-y", getY() + "");
				config.set("main-frame-width", getWidth() + "");
				config.set("main-frame-height", getHeight() + "");
				config.save();
			} catch (IOException ignored) {}
			finally {
				dispose();
				setVisible(false);
				thread.terminate();
			}
			}
		});

		thread = new InnerThread();
		thread.start();
	}

	public void play(String id) {
		try (RubusClient rubusClient = new RubusClient(this::buildSocket)) {
			if (player != null) {
				player.detach(fetchController);
				fetchController.close();
				player.detach(audioController);
				audioPlayer.terminate();
				player.setBuffer(new EncodedPlaybackPiece[0]);
				((Player) player).setVisible(false);
			}
			RubusRequest request = RubusRequest.newBuilder().INFO(id).build();
			RubusResponse response = rubusClient.send(request, 10000);
			MediaInfo mediaInfo = response.INFO();
			request = RubusRequest.newBuilder().FETCH(id, 0, 1).build();
			response = rubusClient.send(request, 10000);
			byte[] audio = response.FETCH().audio()[0];
			AudioFormat audioFormat = AudioSystem.getAudioFileFormat(new ByteArrayInputStream(audio)).getFormat();

			fetchController = new FetchController(this::buildSocket, id);
			audioPlayer = new AudioPlayer(audioFormat);
			audioController = new AudioPlayerController(audioPlayer);
			player = new Player(0, mediaInfo);
			player.attach(fetchController);
			player.attach(audioController);
			player.sendNotification();

			bagLayout.setConstraints((Player) player, constraints);
			add((Player) player);
			revalidate();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private RubusSocket buildSocket() {
		try {
			Path configPath = Path.of(System.getProperty("user.home"), ".rubus", "client_config");
			Config config = new Config(configPath);
			URI uri = new URI(config.get("server-uri"));
			String protocol = uri.getScheme();
			if (protocol.equals("tcp")) {
				return new TCPRubusSocket(InetAddress.getByName(uri.getHost()), uri.getPort());
			} else {
				throw new IllegalArgumentException("Protocol " + protocol + " isn't supported");
			}
		} catch (Exception e) {
			throw new RubusSocketConstructionException(e.getMessage(), e);
		}
	}

	public static void main(String[] args) throws InvocationTargetException, InterruptedException, IOException {
		Path configPath = Path.of(System.getProperty("user.home"), ".rubus", "client_config");
		Config config;
		if (Files.notExists(configPath)) {
			config = Config.create(configPath,
				"server-uri", "tcp://localhost:54300",
				"server-uri", "tcp://localhost:54300",
				"main-frame-x", "0",
				"main-frame-y", "0",
				"main-frame-width", "1920",
				"main-frame-height", "1080"
			);
		} else {
			config = new Config(configPath);
		}

		String x = config.get("main-frame-x");
		String y = config.get("main-frame-y");
		String width = config.get("main-frame-width");
		String height = config.get("main-frame-height");

		SwingUtilities.invokeAndWait(() -> {
			MainFrame mw = new MainFrame();
			mw.setVisible(true);
			mw.setBounds(
				Integer.parseInt(x),
				Integer.parseInt(y),
				Integer.parseInt(width),
				Integer.parseInt(height)
			);
		});
	}

	private class InnerThread extends Thread {
		private boolean isTerminated = false;

		@Override
		public void run() {
			GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			DisplayMode displayMode = graphicsDevice.getDisplayMode();
			int refreshRate = displayMode.getRefreshRate();
			while (!isTerminated) {
				repaint();
				try {
					Thread.sleep(Duration.of(1_000_000_000 / refreshRate, ChronoUnit.NANOS));
				} catch (InterruptedException ignored) {}
			}
		}

		public void terminate() {
			isTerminated = true;
		}
	}
}
