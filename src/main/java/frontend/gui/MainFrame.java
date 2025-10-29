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

package frontend.gui;

import frontend.controllers.AudioPlayerController;
import frontend.controllers.FetchController;
import frontend.models.MediaInfo;
import frontend.interactors.*;
import frontend.decoders.Decoder;
import frontend.decoders.VideoDecoder;
import frontend.gui.mediasearch.MediaSearchDialog;
import frontend.gui.settings.SettingsDialog;
import frontend.gui.settings.SettingsTabs;
import frontend.network.RubusClient;
import frontend.network.RubusResponse;
import frontend.network.RubusRequest;
import frontend.configuration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

public class MainFrame extends JFrame {

	private final Logger logger = LoggerFactory.getLogger(MainFrame.class);

	private final InnerThread thread;
	private final GridBagLayout bagLayout = new GridBagLayout();
	private final GridBagConstraints constraints = new GridBagConstraints();
	private final Supplier<RubusClient> rubusClientSupplier;
	private final Config config;
	private final WatchHistory watchHistory;
	private final VideoDecoder vd;

	private PlayerInterface player = null;
	private FetchController fetchController = null;
	private AudioPlayerInterface audioPlayer = null;
	private AudioPlayerController audioController = null;
	private WatchHistoryRecorder watchHistoryRecorder = null;

	public MainFrame(
		Config config,
		Supplier<RubusClient> rubusClientSupplier,
		WatchHistory watchHistory,
		Supplier<SettingsTabs> settingsTabsSupplier,
		VideoDecoder videoDecoder
	) {
		assert config != null && rubusClientSupplier != null && watchHistory != null && videoDecoder != null;

		this.watchHistory = watchHistory;
		this.config = config;
		this.rubusClientSupplier = rubusClientSupplier;
		vd = videoDecoder;
		setLayout(bagLayout);
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1;
		constraints.weighty = 1;

		MainFrameMenuBar menuBar = new MainFrameMenuBar();
		setJMenuBar(menuBar);

		menuBar.reloadButton().addActionListener(actionEvent -> {
			try {
				if (fetchController != null) {
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
			new MediaSearchDialog(this, rubusClientSupplier, watchHistory);
		});

		menuBar.settingsItem().addActionListener(actionEvent -> {
			SettingsDialog settingsDialog = new SettingsDialog(this, settingsTabsSupplier.get());
			settingsDialog.setVisible(true);
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
					if (player != null) player.close();
				} catch (Exception exception) {
					logger.warn("Exception occurred while closing {}", MainFrame.this, exception);
				}
				finally {
					dispose();
					setVisible(false);
					thread.terminate();
				}
				logger.info("{} closed", MainFrame.this);
			}
		});

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		thread = new InnerThread();
		thread.start();

		logger.debug(
			"""
			{} instantiated, Config: {}, RubusClientSupplier: {}, WatchHistory: {}, SettingsTabSupplier: {}, \
			VideoDecoder: {}""",
			this,
			config,
			rubusClientSupplier,
			watchHistory,
			settingsTabsSupplier,
			videoDecoder
		);
	}

	public void play(String id, int progress) {
		try (RubusClient rubusClient = rubusClientSupplier.get()) {
			RubusRequest request = rubusClient.getRequestBuilder().INFO(id).build();
			RubusResponse response = rubusClient.send(request, 10000);
			MediaInfo mediaInfo = response.INFO();

			request = rubusClient.getRequestBuilder().FETCH(id, 0, 1).build();
			response = rubusClient.send(request, 10000);
			byte[] audio = response.FETCH().audio()[0];
			AudioFormat audioFormat = AudioSystem.getAudioFileFormat(new ByteArrayInputStream(audio)).getFormat();
			audioPlayer = new AudioPlayer(audioFormat);

			if (player != null) {
				fetchController.purge();
				Decoder.StreamContext sc = vd.getStreamContextNow();
				vd.purgeAndFlush();
				if (sc != null) sc.close();
				player.purge();

				player.setVideoDuration(mediaInfo.duration());
				player.setProgress(progress);
				audioController.getAudioPlayer().terminate();
				audioController.setAudioPlayer(audioPlayer);
				player.attach(audioController);
				fetchController.setMediaId(id);
				player.attach(fetchController);
				watchHistoryRecorder.setMediaId(id);
				player.attach(watchHistoryRecorder);

				player.sendNotification();
			} else {
				config.action(c -> {
					int bufferSize = Integer.parseInt(c.get("buffer-size"));
					int minimumBatchSize = Integer.parseInt(c.get("minimum-batch-size"));
					fetchController = new FetchController(rubusClientSupplier, id, bufferSize, minimumBatchSize);
					return null;
				});
				audioController = new AudioPlayerController(audioPlayer);
				player = new Player(progress, vd, mediaInfo.duration());
				watchHistoryRecorder = new WatchHistoryRecorder(watchHistory, id);
				player.attach(fetchController);
				player.attach(audioController);
				player.attach(watchHistoryRecorder);
				player.sendNotification();

				bagLayout.setConstraints((Player) player, constraints);
				add((Player) player);
				revalidate();
			}
		} catch (Exception e) {
			logger.error("{} failed to play media", this, e);
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}

		logger.info("{} plays media with {} id and {} initial progress", this, id, progress);
	}

	public void display() {
		setVisible(true);
	}

	private class InnerThread extends Thread {

		private Logger logger = LoggerFactory.getLogger(InnerThread.class);

		private boolean isTerminated = false;

		private InnerThread() {
			logger.debug("{} instantiated", this);
		}

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
