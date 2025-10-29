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

package backend.stubs;

import backend.models.Viewer;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.UUID;

public class ViewerStub implements Viewer {

	public UUID id;

	public String name;

	public boolean hasAdminPrivileges;

	public Map<String, String> credentials;

	public ViewerStub(UUID id, String name, boolean hasAdminPrivileges, Map<String, String> credentials) {
		this.id = id;
		this.name = name;
		this.hasAdminPrivileges = hasAdminPrivileges;
		this.credentials = credentials;
	}

	public static Viewer getRegularViewer() {
		return new ViewerStub(UUID.randomUUID(), "regular_viewer", false, Map.of());
	}

	public static Viewer getPrivilegedViewer() {
		return new ViewerStub(UUID.randomUUID(), "privileged_viewer", true, Map.of());
	}

	@Nonnull
	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean hasAdminPrivileges() {
		return hasAdminPrivileges;
	}

	@Nonnull
	@Override
	public Map<String, String> getCredentials() {
		return credentials;
	}
}
