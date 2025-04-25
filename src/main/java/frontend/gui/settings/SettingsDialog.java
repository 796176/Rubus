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
import frontend.gui.CenteredDialog;
import frontend.gui.colors.Colors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class SettingsDialog extends CenteredDialog {

	private JPanel currentTab;

	private TabPanel currentTabPanel;

	private GridBagConstraints constraints;

	private GridBagLayout bagLayout;

	private JScrollPane rScrollPane;

	public SettingsDialog(Frame parent, Config config) {
		super(parent, "Settings", true, (int) (parent.getWidth() / 1.2), (int) (parent.getHeight() / 1.2));
		assert config != null;

		try {
			bagLayout = new GridBagLayout();
			setLayout(bagLayout);
			constraints = new GridBagConstraints();
			constraints.weighty = 1;
			constraints.gridheight = GridBagConstraints.REMAINDER;
			constraints.fill = GridBagConstraints.BOTH;

			SettingsTabs settingsTabs = new SettingsTabs(config);
			currentTab = settingsTabs.getAllTabs()[0];
			currentTab.setBackground(Colors.getInstance().settingTabSelected());

			JScrollPane lScrollPane = new JScrollPane(settingsTabs);
			constraints.weightx = 1;
			bagLayout.setConstraints(lScrollPane, constraints);
			add(lScrollPane);

			currentTabPanel = settingsTabs.getAssociatedTabPanel(currentTab);
			rScrollPane = new JScrollPane(currentTabPanel);
			constraints.weightx = 3;
			bagLayout.setConstraints(rScrollPane, constraints);
			add(rScrollPane);

			for (JPanel tab : settingsTabs.getAllTabs()) {
				tab.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent mouseEvent) {
						JPanel tab = (JPanel) mouseEvent.getComponent();
						if (tab != currentTab) {
							currentTab.setBackground(Colors.getInstance().settingsTab());
							currentTab = tab;
							currentTab.setBackground(Colors.getInstance().settingTabSelected());
							try {
								currentTabPanel.save();
							} catch (IOException e) {
								showIOExceptionDialog(e);
							}
							currentTabPanel = settingsTabs.getAssociatedTabPanel(currentTab);
							rScrollPane.setViewportView(currentTabPanel);
						}
					}
				});
			}

			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent windowEvent) {
					try {
						currentTabPanel.save();
					} catch (IOException e) {
						showIOExceptionDialog(e);
						setVisible(false);
					}
				}
			});
			setVisible(true);
		} catch (IOException e) {
			showIOExceptionDialog(e);
			setVisible(false);
		}
	}

	private void showIOExceptionDialog(IOException e) {
		JOptionPane.showMessageDialog(
			SettingsDialog.this,
			e.getMessage(),
			"IOException",
			JOptionPane.ERROR_MESSAGE
		);
	}
}
