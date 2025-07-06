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

import frontend.gui.colors.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SettingsTabs extends JPanel {

	private final Logger logger = LoggerFactory.getLogger(SettingsTabs.class);

	private final JPanel[] tabs;

	private final HashMap<JPanel, TabPanel> associatedTabPanels = new HashMap<>();

	public SettingsTabs(TabPanel[] tabPanels) {
		assert tabPanels != null;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		ArrayList<JPanel> settingsTabs = new ArrayList<>();
		for (TabPanel tabPanel: tabPanels) {
			JPanel tab = createTab(tabPanel.getName());
			associatedTabPanels.put(tab, tabPanel);
			add(tab);
			add(Box.createRigidArea(new Dimension(0, 10)));
			settingsTabs.add(tab);
		}
		tabs = settingsTabs.toArray(new JPanel[0]);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizeTabs();
				revalidate();
			}
		});

		if (logger.isDebugEnabled()) {
			logger.debug("{} instantiated, tab panels: {}", this, Arrays.toString(tabPanels));
		}
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
		return tabs;
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
