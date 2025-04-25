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

package frontend.gui.settings;

import common.Config;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class PlayerTabPanel extends TabPanel {

	private final Config config;

	private final Font categoryFont;

	private final JTextField bufferSizeTF;

	private final JTextField batchSizeTF;

	public PlayerTabPanel(Config config) {
		assert config != null;

		this.config = config;
		categoryFont = new Font(getFont().getName(), Font.BOLD, (int) Math.ceil((double) getFont().getSize() * 1.3));

		GridBagLayout bagLayout = new GridBagLayout();
		setLayout(bagLayout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.insets = new Insets(50, 50, 0, 50);

		JLabel fetchingCategory = new JLabel("Fetching");
		fetchingCategory.setFont(categoryFont);
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(fetchingCategory, constraints);
		add(fetchingCategory);

		JLabel bufferSizeLabel = new JLabel("Buffer Size");
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		bagLayout.setConstraints(bufferSizeLabel, constraints);
		add(bufferSizeLabel);
		bufferSizeTF = new JTextField(config.get("buffer-size"));
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(bufferSizeTF, constraints);
		add(bufferSizeTF);

		JLabel batchSizeLabel = new JLabel("Minimum Batch Size");
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		bagLayout.setConstraints(batchSizeLabel, constraints);
		add(batchSizeLabel);
		batchSizeTF = new JTextField(config.get("minimum-batch-size"));
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(batchSizeTF, constraints);
		add(batchSizeTF);

		Component rigidArea = Box.createRigidArea(new Dimension(1, 1));
		constraints.weighty = 1;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(rigidArea, constraints);
		add(rigidArea);
	}

	@Override
	public void save() throws IOException {
		synchronized (config) {
			config.set("buffer-size", sanitizeValue(bufferSizeTF.getText()));
			config.set("minimum-batch-size", sanitizeValue(batchSizeTF.getText()));
			config.save();
		}
	}
}
