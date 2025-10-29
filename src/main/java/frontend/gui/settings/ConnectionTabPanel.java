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

package frontend.gui.settings;

import frontend.interactors.WatchHistory;
import frontend.configuration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ConnectionTabPanel extends TabPanel {

	private final Logger logger = LoggerFactory.getLogger(ConnectionTabPanel.class);

	private final Config config;

	private final WatchHistory wh;

	private final Font categoryFont;

	private final Font finePrint;

	private final JTextField hostNameTF;

	private final JTextField hostPortTF;

	private final JComboBox<String> transportLayerCB;

	private final JComboBox<String> secureConnectionCB;

	public ConnectionTabPanel(Config config, WatchHistory watchHistory) {
		assert config != null && watchHistory != null;

		this.config = config;
		wh = watchHistory;
		categoryFont = new Font(getFont().getName(), Font.BOLD, (int) Math.ceil((double) getFont().getSize() * 1.3));
		finePrint = new Font(getFont().getName(), Font.ITALIC, (int) Math.floor((double) getFont().getSize() / 1.3));

		GridBagLayout bagLayout = new GridBagLayout();
		setLayout(bagLayout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.insets = new Insets(50, 50, 0, 50);

		JLabel serverCategoryLabel = new JLabel("Host");
		serverCategoryLabel.setFont(categoryFont);
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(serverCategoryLabel, constraints);
		add(serverCategoryLabel);
		JLabel serverCategoryPSLabel =
			new JLabel("Changing these settings will automatically clear the watch history");
		serverCategoryPSLabel.setFont(finePrint);
		constraints.insets = new Insets(0, 50, 0, 0);
		bagLayout.setConstraints(serverCategoryPSLabel, constraints);
		add(serverCategoryPSLabel);

		JLabel hostNameLabel = new JLabel("Host Name");
		constraints.insets = new Insets(50, 50, 0, 50);
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		bagLayout.setConstraints(hostNameLabel, constraints);
		add(hostNameLabel);
		hostNameTF = new JTextField(config.get("bind-address"));
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(hostNameTF, constraints);
		add(hostNameTF);

		JLabel hostPortLabel = new JLabel("Host Port");
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		constraints.fill = GridBagConstraints.NONE;
		bagLayout.setConstraints(hostPortLabel, constraints);
		add(hostPortLabel);
		hostPortTF = new JTextField(config.get("listening-port"));
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(hostPortTF, constraints);
		add(hostPortTF);

		JLabel protocolGroupLabel = new JLabel("Protocol");
		protocolGroupLabel.setFont(categoryFont);
		constraints.insets = new Insets(100, 50, 0, 50);
		bagLayout.setConstraints(protocolGroupLabel, constraints);
		add(protocolGroupLabel);

		JLabel transportLayerLabel = new JLabel("Transport Layer");
		constraints.insets = new Insets(50, 50, 0, 50);
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		bagLayout.setConstraints(transportLayerLabel, constraints);
		add(transportLayerLabel);
		transportLayerCB = new JComboBox<>(new String[] { "tcp" });
		transportLayerCB.setSelectedItem(config.get("connection-protocol"));
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(transportLayerCB, constraints);
		add(transportLayerCB);

		JLabel secureConnectionLabel = new JLabel("Secure Connection");
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		bagLayout.setConstraints(secureConnectionLabel, constraints);
		add(secureConnectionLabel);
		secureConnectionCB = new JComboBox<>(new String[] { "Required", "Not Required", "Disabled" });
		boolean secureConnectionEnabled = Boolean.parseBoolean(config.get("secure-connection-enabled"));
		boolean secureConnectionRequired = Boolean.parseBoolean(config.get("secure-connection-required"));
		if (secureConnectionEnabled) {
			if (secureConnectionRequired) secureConnectionCB.setSelectedItem("Required");
			else secureConnectionCB.setSelectedItem("Not Required");
		} else secureConnectionCB.setSelectedItem("Disabled");
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(secureConnectionCB, constraints);
		add(secureConnectionCB);

		Component rigidArea = Box.createRigidArea(new Dimension(1, 1));
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.weighty = 1;
		bagLayout.setConstraints(rigidArea, constraints);
		add(rigidArea);

		logger.debug("{} instantiated, Config: {}, WatchHistory: {}", this, config, watchHistory);
	}

	@Override
	public void save() throws IOException {
		String hostName = sanitizeValue(hostNameTF.getText());
		String hostPort = sanitizeValue(hostPortTF.getText());
		config.action((c) -> {
			String oldBindAddress = c.get("bind-address");
			String oldListeningPort = c.get("listening-port");
			if (!(oldBindAddress.equals(hostName) && oldListeningPort.equals(hostPort))) wh.purge();
			c.set("bind-address", hostName);
			c.set("listening-port", hostPort);

			c.set("connection-protocol", (String) transportLayerCB.getSelectedItem());
			String secureConnectionCBValue = (String) secureConnectionCB.getSelectedItem();
			if (secureConnectionCBValue.equals("Disabled")) {
				c.set("secure-connection-enabled", "false");
			} else {
				c.set("secure-connection-enabled", "true");
				if (secureConnectionCBValue.equals("Required")) c.set("secure-connection-required", "true");
				else c.set("secure-connection-required", "false");
			}
			c.save();
			return null;
		});
	}

	@Override
	public String getName() {
		return "Connection";
	}
}
