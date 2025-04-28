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
import common.net.response.body.MediaInfo;
import frontend.*;
import frontend.gui.mediasearch.MediaSearchDialog;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

public class MainFrame extends JFrame {
	private final InnerThread thread;
	private final GridBagLayout bagLayout = new GridBagLayout();
	private final GridBagConstraints constraints = new GridBagConstraints();
	private final Supplier<RubusSocket> rubusSocketSupplier;
	private final Config config;
	private final WatchHistory watchHistory;

	private PlayerInterface player = null;
	private FetchController fetchController = null;
	private AudioPlayerInterface audioPlayer = null;
	private AudioPlayerController audioController = null;
	private WatchHistoryRecorder watchHistoryRecorder = null;
	public MainFrame(Config config, Supplier<RubusSocket> rubusSocketSupplier, WatchHistory watchHistory) {
		assert config != null && rubusSocketSupplier != null && watchHistory != null;

		this.watchHistory = watchHistory;
		this.config = config;
		this.rubusSocketSupplier = rubusSocketSupplier;
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
					fetchController.setSocketSupplier(rubusSocketSupplier);
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
			new MediaSearchDialog(this, rubusSocketSupplier, watchHistory);
		});

		menuBar.settingsItem().addActionListener(actionEvent -> {
			new frontend.gui.settings.SettingsDialog(this, config, watchHistory);
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
		try (RubusClient rubusClient = new RubusClient(rubusSocketSupplier)) {
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

			config.action(c -> {
				int bufferSize = Integer.parseInt(c.get("buffer-size"));
				int minimumBatchSize = Integer.parseInt(c.get("minimum-batch-size"));
				fetchController = new FetchController(rubusSocketSupplier, id, bufferSize, minimumBatchSize);
				return null;
			});
			audioPlayer = new AudioPlayer(audioFormat);
			audioController = new AudioPlayerController(audioPlayer);
			int progress = watchHistory.getProgress(id);
			if (progress == -1) progress = 0;
			player = new Player(progress, mediaInfo);
			watchHistoryRecorder = new WatchHistoryRecorder(watchHistory, id);
			player.attach(fetchController);
			player.attach(audioController);
			player.attach(watchHistoryRecorder);
			player.sendNotification();

			bagLayout.setConstraints((Player) player, constraints);
			add((Player) player);
			revalidate();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void display() {
		setVisible(true);
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
