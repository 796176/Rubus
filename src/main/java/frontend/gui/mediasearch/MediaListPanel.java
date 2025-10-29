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

package frontend.gui.mediasearch;

import frontend.models.MediaList;
import frontend.interactors.WatchHistory;
import frontend.gui.MainFrame;
import frontend.gui.colors.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class MediaListPanel extends JPanel {

	private final Logger logger = LoggerFactory.getLogger(MediaListPanel.class);

	private final WatchHistory wh;

	private final Font finePrint;

	public MediaListPanel(
		MediaSearchDialog mediaSearchDialog,
		MainFrame mainFrame,
		MediaList list,
		WatchHistory watchHistory
	) {
		assert mediaSearchDialog != null && mainFrame != null && list != null && watchHistory != null;

		wh = watchHistory;
		finePrint = new Font(getFont().getName(), Font.ITALIC, (int) (getFont().getSize() / 1.3));

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		for(Map.Entry<String, String> media: list.media().entrySet()) {
			JPanel mediaPanel = createMediaPanel(media.getValue(), media.getKey());
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
					mainFrame.play(
						(String) mediaPanel.getClientProperty("id"),
						(Integer) mediaPanel.getClientProperty("progress")
					);
				}
			});
			add(mediaPanel);
			add(Box.createRigidArea(new Dimension(0, 10)));
		}

		logger.debug(
			"{} instantiated, MediaSearchDialog: {}, MainFrame: {}, MediaList: {}, WatchHistory: {}",
			this,
			mediaSearchDialog,
			mainFrame,
			list,
			watchHistory
		);
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
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		bagLayout.setConstraints(titleLabel, constraints);
		mediaPanel.add(titleLabel);

		int progress = wh.getProgress(id);
		JLabel progressLabel = progress != -1 ?
			new JLabel("Starts from " + watchProgressFormat(progress)) :
			new JLabel("Not watched yet");
		mediaPanel.putClientProperty("progress", progress != -1 ? progress : 0);
		progressLabel.setFont(finePrint);
		constraints.anchor = GridBagConstraints.SOUTHEAST;
		bagLayout.setConstraints(progressLabel, constraints);
		mediaPanel.add(progressLabel);

		return mediaPanel;
	}


	private String watchProgressFormat(int progress) {
		int hours = progress / 3600;
		progress = progress % 3600;
		int minutes = progress / 60;
		int seconds = progress % 60;
		if (hours == 0) return "%02d:%02d".formatted(minutes, seconds);
		else return "%d:%02d:%02d".formatted(hours, minutes, seconds);
	}
}
