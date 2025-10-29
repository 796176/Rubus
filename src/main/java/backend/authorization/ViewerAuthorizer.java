/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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

package backend.authorization;

import backend.models.Viewer;
import jakarta.annotation.Nonnull;

/**
 * ViewerAuthorizer describes an authorization interface to check if a viewer is allowed to perform a particular action
 * based on its type.
 */
public interface ViewerAuthorizer {

	/**
	 * Returns true if the viewer is allowed to perform the action, false otherwise.
	 * @param viewer the viewer performing the action
	 * @param type the action type
	 * @return true if the viewer is allowed to perform the action, false otherwise
	 */
	boolean validate(@Nonnull Viewer viewer, @Nonnull ActionType type);
}
