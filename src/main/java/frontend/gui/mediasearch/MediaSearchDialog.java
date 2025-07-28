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

package frontend.gui.mediasearch;

import common.RubusSocket;
import common.net.response.body.MediaList;
import frontend.RubusClient;
import frontend.RubusRequest;
import frontend.RubusResponse;
import frontend.WatchHistory;
import frontend.gui.CenteredDialog;
import frontend.gui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

public class MediaSearchDialog extends CenteredDialog implements Runnable {

	private final Logger logger = LoggerFactory.getLogger(MediaSearchDialog.class);

	private final MainFrame mainFrame;

	private final Supplier<RubusSocket> rubusSocketSupplier;

	private final WatchHistory watchHistory;

	private final JTextField searchTF;

	private final JScrollPane scrollPane;

	public MediaSearchDialog(MainFrame parent, Supplier<RubusSocket> rubusSocketSupplier, WatchHistory watchHistory) {
		super(parent, "New video", true, parent.getWidth() / 2, parent.getHeight());
		assert rubusSocketSupplier != null && watchHistory != null;

		this.mainFrame = parent;
		this.rubusSocketSupplier = rubusSocketSupplier;
		this.watchHistory = watchHistory;

		GridBagLayout bagLayout = new GridBagLayout();
		setLayout(bagLayout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(50, 50, 0, 50);
		constraints.anchor = GridBagConstraints.NORTH;

		JLabel searchLabel = new JLabel("Search");
		constraints.weightx = 1;
		bagLayout.setConstraints(searchLabel, constraints);
		add(searchLabel);
		searchTF = new JTextField();
		searchTF.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					new Thread(MediaSearchDialog.this).start();
				}
			}
		});
		constraints.weightx = 4;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(searchTF, constraints);
		add(searchTF);

		scrollPane = new JScrollPane();
		JScrollBar verticalSB = scrollPane.getVerticalScrollBar();
		verticalSB.setUnitIncrement(verticalSB.getUnitIncrement() * 10);
		constraints.weighty = 1;
		constraints.weightx = 1;
		constraints.insets = new Insets(50, 50, 50, 50);
		constraints.fill = GridBagConstraints.BOTH;
		bagLayout.setConstraints(scrollPane, constraints);
		add(scrollPane);

		new Thread(this).start();
		setVisible(true);

		logger.debug(
			"{} instantiated, MainFrame: {}, Supplier: {}, WatchHistory: {}",
			this,
			parent,
			rubusSocketSupplier,
			watchHistory
		);
	}

	@Override
	public synchronized void run() {
		try (RubusClient rubusClient = new RubusClient(rubusSocketSupplier)) {
			RubusRequest.Builder requestBuilder = RubusRequest.newBuilder();
			requestBuilder.LIST(searchTF.getText());
			RubusResponse response = rubusClient.send(requestBuilder.build(), 10000);
			MediaList mediaList = response.LIST();

			SwingUtilities.invokeLater(() -> {
				scrollPane.setViewportView(new MediaListPanel(this, mainFrame, mediaList, watchHistory));
			});
		} catch (Exception e) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(
					this,
					e.getMessage(),
					"Network Error",
					JOptionPane.ERROR_MESSAGE
				);
				setVisible(false);
			});

			logger.info("{} couldn't retrieve result from server", this, e);
		}
	}
}
