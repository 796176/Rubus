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

import frontend.exceptions.FetchingException;
import frontend.network.RubusClient;
import frontend.models.MediaList;
import frontend.interactors.WatchHistory;
import frontend.gui.CenteredDialog;
import frontend.gui.MainFrame;
import frontend.network.RubusRequest;
import frontend.network.RubusResponse;
import frontend.network.RubusResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.function.Supplier;

public class MediaSearchDialog extends CenteredDialog implements Runnable {

	private final Logger logger = LoggerFactory.getLogger(MediaSearchDialog.class);

	private final MainFrame mainFrame;

	private final Supplier<RubusClient> rubusCleintSupplier;

	private final WatchHistory watchHistory;

	private final JTextField searchTF;

	private final JScrollPane scrollPane;

	public MediaSearchDialog(MainFrame parent, Supplier<RubusClient> rubusClientSupplier, WatchHistory watchHistory) {
		super(parent, "New video", true, parent.getWidth() / 2, parent.getHeight());
		assert rubusClientSupplier != null && watchHistory != null;

		this.mainFrame = parent;
		this.rubusCleintSupplier = rubusClientSupplier;
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
			rubusClientSupplier,
			watchHistory
		);
	}

	@Override
	public synchronized void run() {
		try (RubusClient rubusClient = rubusCleintSupplier.get()) {
			RubusRequest.Builder requestBuilder = rubusClient.getRequestBuilder();
			requestBuilder.LIST(searchTF.getText());
			RubusResponse response = rubusClient.send(requestBuilder.build(), 10000);
			if (response.getResponseType() != RubusResponseType.OK) {
				throw new IOException(
					"Couldn't fetch data from server, response code: " + response.getResponseType()
				);
			}
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
