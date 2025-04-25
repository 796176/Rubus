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

import javax.swing.*;
import java.io.IOException;

/**
 * TabPanel represents any settings layout containing text fields, combo boxes, etc., which displays the current
 * state of config file parameters, and allows the user to change them and to save that change to the config file.
 */
public abstract class TabPanel extends JPanel {

	/**
	 * Saves the values of text fields, combo boxes, etc. to the config file.
	 * @throws IOException if some I/O exception occurs
	 */
	public void save() throws IOException { }

	/**
	 * This method should be called by the subclasses to sanitize the value the user entered manually. It may change
	 * the value so it can be saved to the config file.
	 * @param value the value user entered manually
	 * @return the sanitized value
	 */
	protected String sanitizeValue(String value) {
		return value.replace(" ", "").replace("\n", "");
	}
}
