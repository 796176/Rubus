/*
 * Rubus is an application layer protocol for video and audio streaming and
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

import javax.swing.*;

public class MainFrameMenuBar extends JMenuBar {

	private final JButton reloadButton;

	private final JMenuItem openVideoItem;

	private final JMenuItem settingsItem;

	private final JMenuItem aboutItem;

	public MainFrameMenuBar() {
		reloadButton = new JButton("Reload");
		add(reloadButton);

		JMenu videoMenu = new JMenu("Open");
		openVideoItem = new JMenuItem("Open a new video");
		videoMenu.add(openVideoItem);
		add(videoMenu);

		JMenu settingsMenu = new JMenu("Settings");
		settingsItem = new JMenuItem("Settings");
		settingsMenu.add(settingsItem);
		add(settingsMenu);

		JMenu helpMenu = new JMenu("Help");
		aboutItem = new JMenuItem("About");
		helpMenu.add(aboutItem);
		add(helpMenu);
	}

	public JButton reloadButton() {
		return reloadButton;
	}

	public JMenuItem openVideoItem() {
		return openVideoItem;
	}

	public JMenuItem settingsItem() {
		return settingsItem;
	}

	public JMenuItem aboutItem() {
		return aboutItem;
	}
}
