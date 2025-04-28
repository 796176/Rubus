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

import frontend.WatchHistory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LocalTabPanel extends TabPanel {

	private final WatchHistory wh;

	public LocalTabPanel(WatchHistory watchHistory) {
		assert watchHistory != null;

		wh = watchHistory;

		GridBagLayout bagLayout = new GridBagLayout();
		setLayout(bagLayout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.insets = new Insets(50, 50, 0, 50);

		JLabel clearWHLabel = new JLabel("Clear The Watch History");
		bagLayout.setConstraints(clearWHLabel, constraints);
		add(clearWHLabel);
		JButton clearWHButton = new JButton("Clear");
		clearWHButton.addActionListener(actionEvent -> {
			try {
				wh.purge();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(
					this,
					e.getMessage(),
					"IOException",
					JOptionPane.ERROR_MESSAGE
				);
			}
		});
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(clearWHButton, constraints);
		add(clearWHButton);

		Component rigidArea = Box.createRigidArea(new Dimension(1, 1));
		constraints.weighty = 1;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(rigidArea, constraints);
		add(rigidArea);
	}
}
