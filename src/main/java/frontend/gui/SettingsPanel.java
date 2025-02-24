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

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsPanel extends JPanel {
	public SettingsPanel(SettingsDialog parent) throws IOException {
		assert parent != null;

		Path configPath = Path.of(System.getProperty("user.home"), ".rubus", "client_config");
		String configBody = Files.readString(configPath);
		Pattern pattern = Pattern.compile("server-uri [a-z]+://\\S+:\\d{1,5}");
		Matcher matcher = pattern.matcher(configBody);
		matcher.find();
		String uri = configBody.substring(configBody.indexOf(' ', matcher.start()) + 1, matcher.end());

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
			String newConfigBody = configBody.replace("server-uri " + uri, "server-uri " + newUri);
			try {
				Files.writeString(configPath, newConfigBody);
				parent.setVisible(false);
			} catch (IOException ignored) {}
		});
		bagLayout.setConstraints(saveButton, constraints);
		add(saveButton);
	}
}
