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
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

public class AboutPanel extends JPanel {
	public AboutPanel() {
		GridBagLayout bagLayout = new GridBagLayout();
		setLayout(bagLayout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weighty = 1;
		constraints.weightx = 1;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridheight = GridBagConstraints.REMAINDER;

		JEditorPane textPane = new JEditorPane("text/html", "");
		textPane.setEditable(false);
		textPane.addHyperlinkListener(hyperlinkEvent -> {
			if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				try {
					Desktop desktop = Desktop.getDesktop();
					if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
						desktop.browse(hyperlinkEvent.getURL().toURI());
					} else {
						String os = System.getProperty("os.name").toLowerCase();
						Runtime runtime = Runtime.getRuntime();
						if (os.matches("linux")) {
							runtime.exec(new String[]{"xdg-open", hyperlinkEvent.getURL().toString()});
						}
					}
				} catch (IOException | URISyntaxException ignored) {}
			}
		});
		textPane.setText("""
			Rubus is an application level protocol for video and audio streaming and
			the client and server reference implementations.<br>
			Copyright (C) 2024 Yegore Vlussove<br><br>
			
			Official page: <a href="https://github.com/796176/Rubus">github.com/796176/Rubus</a><br>
			License: Gnu Public License 3<br><br>
			
			FFmpeg: <a href="https://www.ffmpeg.org/">www.ffmpeg.org</a>
			""");
		bagLayout.setConstraints(textPane, constraints);
		add(textPane);
	}
}
