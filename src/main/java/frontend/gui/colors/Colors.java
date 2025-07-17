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

package frontend.gui.colors;

import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Colors declares a set of methods that return look-and-feel dependent color of GUI components.
 */
public interface Colors {

	/**
	 * Returns the background color of the settings tabs in {@link frontend.gui.settings.SettingsTabs}.
	 * @return the background color of the settings tabs
	 */
	Color settingsTab();

	/**
	 * Returns the background color of the selected settings tab.
	 * @return the background color of the selected settings tab
	 */
	Color settingTabSelected();

	/**
	 * Returns the background color of the media panels in {@link frontend.gui.mediasearch.MediaListPanel}
	 * @return the background color of the media panels
	 */
	Color listedMediaPanel();

	/**
	 * Returns the background color of the media panel when the pointer is pointed at it.
	 * @return the background color of the media panel when the pointer is pointed at it
	 */
	Color listedMediaPanelFocused();

	/**
	 * Returns a concrete instance of Colors depending on the current look-and-feel.
	 * @return a concrete instance of Colors
	 * @throws RuntimeException if the look-and-feel is not supported
	 */
	static Colors getInstance() {
		String laf = UIManager.getLookAndFeel().getClass().getName();
		switch (laf) {
			case "com.formdev.flatlaf.FlatDarkLaf" -> { return new FlatDarkLaf(); }
			case "com.formdev.flatlaf.FlatLightLaf" -> { return new FlatLightLaf(); }
			default -> {
				LoggerFactory.getLogger(Colors.class).error("{} doesn't support {}", Colors.class, laf);
				throw new RuntimeException("The " + laf + "look-and-feel is not supported");
			}
		}
	}
}
