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

import common.RubusSocket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class OpenVideoDialog extends JDialog {
	private final MainFrame mainFrame;

	public OpenVideoDialog(MainFrame mainFrame, RubusSocket socket) {
		super(mainFrame, "Open a new video", true);
		assert mainFrame != null && socket != null;

		this.mainFrame = mainFrame;
		setSize(600, 1200);
		Rectangle parentBounds = mainFrame.getBounds();

		setBounds(
			parentBounds.x + (parentBounds.width - getWidth()) / 2,
			parentBounds.y + (parentBounds.height - getHeight()) / 2,
			getWidth(),
			getHeight()
		);

		JScrollPane scrollPane = new JScrollPane(new OpenVideoPanel(this, socket));
		setContentPane(scrollPane);
	}

	public void handleLabelClick(MouseEvent e) {
		JLabel label = (JLabel) e.getSource();
		mainFrame.play((String) label.getClientProperty("id"));
		setVisible(false);
	}
}
