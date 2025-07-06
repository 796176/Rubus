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

import common.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class GeneralTabPanel extends TabPanel {

	private final Logger logger = LoggerFactory.getLogger(GeneralTabPanel.class);

	private final JComboBox<String> languageCB;

	private final JComboBox<String> lookAndFeelCB;

	private final Config config;

	public GeneralTabPanel(Config config) {
		assert config != null;

		this.config = config;

		GridBagLayout bagLayout = new GridBagLayout();
		setLayout(bagLayout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1;
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.insets = new Insets(50, 50, 0, 50);

		JLabel languageLabel = new JLabel("Language");
		bagLayout.setConstraints(languageLabel, constraints);
		add(languageLabel);
		languageCB = new JComboBox<>(new String[]{ "english" });
		languageCB.setSelectedItem(config.get("interface-language"));
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(languageCB, constraints);
		add(languageCB);

		JLabel lookAndFeelLabel = new JLabel("Look&Feel");
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		bagLayout.setConstraints(lookAndFeelLabel, constraints);
		add(lookAndFeelLabel);
		lookAndFeelCB = new JComboBox<>(new String[]{ "FlatDarkLaf", "FlatLightLaf" });
		String lookAndFeelClassName = config.get("look-and-feel");
		lookAndFeelCB.setSelectedItem(
			lookAndFeelClassName.substring(lookAndFeelClassName.lastIndexOf('.') + 1)
		);
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(lookAndFeelCB, constraints);
		add(lookAndFeelCB);

		Component c = Box.createRigidArea(new Dimension(1, 1));
		constraints.weighty = 1;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(c, constraints);
		add(c);

		logger.debug("{} instantiated, Config: {}", this, config);
	}

	@Override
	public void save() throws IOException {
		config.set("interface-language", (String) languageCB.getSelectedItem());
		config.set("look-and-feel", "com.formdev.flatlaf." + lookAndFeelCB.getSelectedItem());
		config.save();
	}

	@Override
	public String getName() {
		return "General";
	}
}
