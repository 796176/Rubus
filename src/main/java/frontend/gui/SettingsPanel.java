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

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class SettingsPanel extends JPanel {
	public SettingsPanel(SettingsDialog parent) throws IOException {
		assert parent != null;

		Path configPath = Path.of(System.getProperty("user.home"), ".rubus", "client_config");
		Config config = new Config(configPath);
		String uri = config.get("server-uri");

		GridBagLayout bagLayout = new GridBagLayout();
		setLayout(bagLayout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1;
		constraints.weighty = 1;

		constraints.anchor = GridBagConstraints.WEST;
		JLabel uriLabel = new JLabel("URI");
		bagLayout.setConstraints(uriLabel, constraints);
		add(uriLabel);

		constraints.anchor = GridBagConstraints.EAST;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		JTextField uriTextField = new JTextField(uri);
		bagLayout.setConstraints(uriTextField, constraints);
		add(uriTextField);

		constraints.anchor = GridBagConstraints.CENTER;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(actionEvent -> {
			String newUri = uriTextField.getText();
			config.set("server-uri", newUri);
			try {
				config.save();
				parent.setVisible(false);
			} catch (IOException ignored) {}
		});
		bagLayout.setConstraints(saveButton, constraints);
		add(saveButton);
	}
}
