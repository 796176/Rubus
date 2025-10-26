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

package backend.models;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * A concrete implementation of {@link Viewer}.
 */
public class DefaultViewer implements Viewer {

	private final Logger logger = LoggerFactory.getLogger(DefaultViewer.class);

	private final UUID id;

	private final String name;

	private final boolean hasAdminPrivileges;

	private final Map<String, String> credentials;

	/**
	 * Constructs an instance of this class.
	 * @param id the viewer id
	 * @param name the viewer name
	 * @param hasAdminPrivileges whether this viewer has administrator privileges
	 * @param credentials the credentials
	 */
	public DefaultViewer(
		@Nonnull UUID id, String name, boolean hasAdminPrivileges, @Nonnull Map<String, String> credentials
	) {
		this.id = id;
		this.name = name;
		this.hasAdminPrivileges = hasAdminPrivileges;
		this.credentials = credentials;

		logger.debug(
			"{} instantiated, id: {}, name: {}, hasAdminPrivileges: {}, credentials: {}",
			this,
			id,
			name,
			hasAdminPrivileges,
			credentials
		);
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
