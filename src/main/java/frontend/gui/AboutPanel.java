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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class AboutPanel extends JPanel {

	private final Logger logger = LoggerFactory.getLogger(AboutPanel.class);

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
						desktop.browse(URI.create(hyperlinkEvent.getURL().toString()));
					} else {
						String os = System.getProperty("os.name").toLowerCase();
						Runtime runtime = Runtime.getRuntime();
						if (os.matches("linux")) {
							runtime.exec(new String[]{"xdg-open", hyperlinkEvent.getURL().toString()});
						}
					}
				} catch (IOException e) {
					logger.info("{} couldn't open hyperlink", this, e);
				}
			}
		});

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String version = "Unknown";
		try (InputStream is = classLoader.getResourceAsStream("version")) {
			version = new String(is.readAllBytes());
		} catch (RuntimeException | IOException ignored) { }
		textPane.setText("""
			<h3>Rubus %s</h3><br>
			Rubus is an application layer protocol for video and audio streaming and
			the client and server reference implementations.<br>
			Copyright (C) 2024-2025 Yegore Vlussove<br><br>
			
			Official page: <a href="https://github.com/796176/Rubus">github.com/796176/Rubus</a><br>
			License: Gnu Public License 3<br><br>
			
			This software included following frameworks/libraries:<br>
			&emsp;&emsp;FFmpeg: <a href="https://www.ffmpeg.org/">www.ffmpeg.org</a><br>
			&emsp;&emsp;Spring Framework: <a href="https://spring.io/">spring.io</a><br>
			&emsp;&emsp;Log4J: <a href="https://logging.apache.org/log4j/2.x/">logging.apache.org</a><br>
			&emsp;&emsp;FlatLaf: <a href="https://www.formdev.com/flatlaf/">www.formdev.com</a><br>
			""".formatted(version));
		bagLayout.setConstraints(textPane, constraints);
		add(textPane);

		logger.debug("{} instantiated", this);
	}
}
