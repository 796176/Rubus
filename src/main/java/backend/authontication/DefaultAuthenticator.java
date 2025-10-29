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

package backend.authontication;

import backend.exceptions.AuthenticationException;
import backend.models.DefaultViewer;
import backend.models.RequestOriginator;
import backend.models.Viewer;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * A concrete implementation of {@link Authenticator} that instantiates a new viewer without administrator privileges.
 */
public class DefaultAuthenticator implements Authenticator{

	private final Logger logger = LoggerFactory.getLogger(DefaultAuthenticator.class);

	/**
	 * Constructs an instance of this class.
	 */
	public DefaultAuthenticator() {
		logger.debug("{} instantiated", this);
	}

	/**
	 * Performs authentication of the provided {@link RequestOriginator}. A new {@link Viewer} is instantiated
	 * even if the {@link RequestOriginator} is the same object.
	 * @param requestOriginator the {@link RequestOriginator} instance
	 * @return a {@link Viewer} instance
	 * @throws AuthenticationException if authentication fails
	 */
	@Nonnull
	@Override
	public Viewer authenticate(RequestOriginator requestOriginator) {
		UUID viewerId = UUID.randomUUID();
		Viewer viewer = new DefaultViewer(viewerId, viewerId.toString(), false, Map.of());
		logger.info("{} mapped {} request originator id to {} viewer", this, requestOriginator.getId(), viewer);
		return viewer;
	}
}
