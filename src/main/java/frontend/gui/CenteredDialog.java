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

package frontend.gui;

import javax.swing.*;
import java.awt.*;

/**
 * CenteredDialog is meant to be subclassed by other gui dialogs if they need to be centered relative to their parent.
 */
public abstract class CenteredDialog extends JDialog {
	public CenteredDialog(Frame parent, String title, boolean modal, int width, int height) {
		super(parent, title, modal);
		assert parent != null && width > 0 && height > 0;

		Rectangle parentBounds = parent.getBounds();
		setBounds(
			parentBounds.x + (parentBounds.width - width) / 2,
			parentBounds.y + (parentBounds.height - height) / 2,
			width,
			height
		);
	}
}
