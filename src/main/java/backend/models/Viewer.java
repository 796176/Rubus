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

package backend.models;

import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.UUID;

/**
 * The Viewer interface provides access to user-specific information.
 */
public interface Viewer {

	/**
	 * Returns the viewer id.
	 * @return the viewer id
	 */
	@Nonnull
	UUID getId();

	/**
	 * Returns the name of the user.
	 * @return the name of the user
	 */
	String getName();

	/**
	 * Returns true if this viewer has administrator privileges.
	 * @return true if this viewer has administrator privileges
	 */
	boolean hasAdminPrivileges();

	/**
	 * Returns credentials associated with this viewer.
	 * @return credentials associated with this viewer
	 */
	@Nonnull
	Map<String, String> getCredentials();
}
