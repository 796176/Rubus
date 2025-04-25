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

package frontend.gui.mediasearch;

import common.net.response.body.MediaList;
import frontend.gui.MainFrame;
import frontend.gui.colors.Colors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MediaListPanel extends JPanel {
	public MediaListPanel(MediaSearchDialog mediaSearchDialog, MainFrame mainFrame, MediaList list) {
		assert mediaSearchDialog != null && mainFrame != null && list != null;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		for(int i = 0; i < list.ids().length; i++) {
			JPanel mediaPanel = createMediaPanel(list.titles()[i], list.ids()[i]);
			mediaPanel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					mediaPanel.setBackground(Colors.getInstance().listedMediaPanelFocused());
				}

				@Override
				public void mouseExited(MouseEvent e) {
					mediaPanel.setBackground(Colors.getInstance().listedMediaPanel());
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					mediaSearchDialog.setVisible(false);
					JPanel mediaPanel = (JPanel) e.getComponent();
					mainFrame.play((String) mediaPanel.getClientProperty("id"));
				}
			});
			add(mediaPanel);
			add(Box.createRigidArea(new Dimension(0, 10)));
		}
	}

	private JPanel createMediaPanel(String title, String id) {
		JPanel mediaPanel = new JPanel();

		mediaPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
		mediaPanel.setMinimumSize(new Dimension(mediaPanel.getMinimumSize().width, 110));
		mediaPanel.setPreferredSize(new Dimension(mediaPanel.getPreferredSize().width, 110));
		mediaPanel.setBackground(Colors.getInstance().listedMediaPanel());
		mediaPanel.putClientProperty("id", id);

		GridBagLayout bagLayout = new GridBagLayout();
		mediaPanel.setLayout(bagLayout);
		GridBagConstraints constraints = new GridBagConstraints();

		JLabel titleLabel = new JLabel(title);
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.insets = new Insets(10, 10, 10, 10);
		bagLayout.setConstraints(titleLabel, constraints);
		mediaPanel.add(titleLabel);

		return mediaPanel;
	}
}
