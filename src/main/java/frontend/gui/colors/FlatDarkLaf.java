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

package frontend.gui.colors;

import java.awt.*;

/**
 * A concrete implementation of {@link Colors} for the com.formdev.flatlaf.FlatDarkLaf look-and-feel.
 */
public class FlatDarkLaf implements Colors {
	@Override
	public Color settingsTab() {
		return new Color(0x474A4D);
	}

	@Override
	public Color settingTabSelected() {
		return new Color(0x575B5E);
	}

	@Override
	public Color listedMediaPanel() {
		return new Color(0x474A4D);
	}

	@Override
	public Color listedMediaPanelFocused() {
		return new Color(0x575B5E);
	}
}
