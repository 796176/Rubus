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

package backend.authorization;

import backend.models.Viewer;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A concrete implementation of {@link ViewerAuthorizer} that allows any viewer to perform a read action and allows
 * a viewer with administrator privileges to perform a modify action.
 */
public class BasicViewerAuthorizer implements ViewerAuthorizer {

	private final Logger logger = LoggerFactory.getLogger(BasicViewerAuthorizer.class);

	/**
	 * Constructs an instance of this class.
	 */
	public BasicViewerAuthorizer() {
		logger.debug("{} instantiated", this);
	}

	@Override
	public boolean validate(@Nonnull Viewer viewer, @Nonnull ActionType type) {
		return switch (type) {
			case READ -> true;
			case MODIFY -> viewer.hasAdminPrivileges();
		};
	}
}
