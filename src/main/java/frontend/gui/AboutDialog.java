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

public class AboutDialog extends JDialog {
	public AboutDialog(JFrame parent) {
		super(parent, "About Rubus", true);

		setSize(600, 800);

		Rectangle parentBounds = parent.getBounds();
		setBounds(
			parentBounds.x + (parentBounds.width - getWidth()) / 2,
			parentBounds.y + (parentBounds.height - getHeight()) / 2,
			getWidth(),
			getHeight()
		);

		JScrollPane scrollPane = new JScrollPane(new AboutPanel());
		add(scrollPane);
	}
}
