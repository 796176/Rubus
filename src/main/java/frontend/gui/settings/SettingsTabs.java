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
import frontend.gui.colors.Colors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.HashMap;

public class SettingsTabs extends JPanel {

	private final JPanel generalTab;

	private final JPanel connectionTab;

	private final JPanel playerTab;

	private final JPanel localTab;

	private final HashMap<JPanel, TabPanel> associatedTabPanels = new HashMap<>();

	public SettingsTabs(Config config) throws IOException {
		assert config != null;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		generalTab = createTab("General");
		associatedTabPanels.put(generalTab, new GeneralTabPanel(config));
		add(generalTab);
		add(Box.createRigidArea(new Dimension(0, 10)));

		connectionTab = createTab("Connection");
		associatedTabPanels.put(connectionTab, new ConnectionTabPanel(config));
		add(connectionTab);
		add(Box.createRigidArea(new Dimension(0, 10)));

		playerTab = createTab("Player");
		associatedTabPanels.put(playerTab, new PlayerTabPanel(config));
		add(playerTab);
		add(Box.createRigidArea(new Dimension(0, 10)));

		localTab = createTab("Local");
		associatedTabPanels.put(localTab, new LocalTabPanel());
		add(localTab);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizeTabs();
				revalidate();
			}
		});
	}

	private JPanel createTab(String label) {
		JPanel tab = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		tab.setLayout(gridBagLayout);
		tab.setBackground(Colors.getInstance().settingsTab());
		JLabel jLabel = new JLabel(label);
		gridBagLayout.setConstraints(jLabel, new GridBagConstraints());
		tab.add(jLabel);
		return tab;
	}

	public JPanel[] getAllTabs() {
		return new JPanel[] {generalTab, connectionTab, playerTab, localTab};
	}

	public TabPanel getAssociatedTabPanel(JPanel tab) {
		return associatedTabPanels.get(tab);
	}

	private void resizeTabs() {
		for (JPanel tab: getAllTabs()) {
			resizeTab(tab);
		}
	}

	private void resizeTab(JPanel tab) {
		Dimension d = new Dimension(getWidth(), 70);
		tab.setMaximumSize(d);
	}
}
