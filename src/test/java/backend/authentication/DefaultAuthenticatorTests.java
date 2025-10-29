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

package backend.authentication;

import backend.authontication.DefaultAuthenticator;
import backend.models.Viewer;
import backend.stubs.RequestOriginatorStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultAuthenticatorTests {

	DefaultAuthenticator authenticator = new DefaultAuthenticator();

	@Test
	void authenticationTest() {
		RequestOriginatorStub requestOriginator = new RequestOriginatorStub("abcd");

		assertDoesNotThrow(
			() -> {
				Viewer viewer = authenticator.authenticate(requestOriginator);
				assertNotNull(viewer, "Authenticator returned null");
				assertFalse(viewer.hasAdminPrivileges(), "Viewer has administrator privileges");
			},
			"Authentication failed"
		);
	}
}
