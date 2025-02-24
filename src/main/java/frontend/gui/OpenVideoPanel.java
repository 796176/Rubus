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

package frontend.gui;

import common.RubusSocket;
import common.net.response.body.PlaybackList;
import frontend.RubusClient;
import frontend.RubusRequest;
import frontend.RubusResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class OpenVideoPanel extends JPanel implements Runnable {
	private final GridBagLayout bagLayout = new GridBagLayout();
	private final GridBagConstraints constraints = new GridBagConstraints();
	private final ArrayList<JLabel> labels = new ArrayList<>(1024);
	private final JTextField searchField;
	private final RubusSocket socket;
	private final OpenVideoDialog openVideoDialog;

	public OpenVideoPanel(OpenVideoDialog parent, RubusSocket socket) {
		assert parent != null && socket != null;

		this.socket = socket;
		openVideoDialog = parent;
		setLayout(bagLayout);

		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.CENTER;
		searchField = new JTextField();
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					for (JLabel label: labels) {
						label.setVisible(false);
					}
					labels.clear();
					new Thread(OpenVideoPanel.this).start();
				}
			}
		});
		bagLayout.setConstraints(searchField, constraints);
		add(searchField);

		new Thread(this).start();
	}

	@Override
	public synchronized void run(){
		try {
			RubusRequest.Builder requestBuilder = RubusRequest.newBuilder().LIST();
			if (!searchField.getText().isBlank()) requestBuilder.params(searchField.getText());
			RubusResponse response = new RubusClient(socket).send(requestBuilder.build(), 10000);
			PlaybackList playbackList = response.LIST();

			for (int i = 0; i < playbackList.ids().length; i++) {
				JLabel label = new JLabel(playbackList.titles()[i]);
				label.putClientProperty("id", playbackList.ids()[i]);
				label.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						openVideoDialog.handleLabelClick(e);
					}
				});
				bagLayout.setConstraints(label, constraints);
				add(label);
				labels.add(label);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(
				openVideoDialog,
				e.getMessage(),
				"Network Error",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}
}
