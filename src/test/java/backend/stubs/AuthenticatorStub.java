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

package backend.stubs;

import backend.authontication.Authenticator;
import backend.exceptions.NotImplementedExceptions;
import backend.models.RequestOriginator;
import backend.models.Viewer;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

public class AuthenticatorStub implements Authenticator {

	public Function<RequestOriginator, Viewer> authenticateFunction = ro -> {
		throw new NotImplementedExceptions();
	};

	@Nonnull
	@Override
	public Viewer authenticate(RequestOriginator requestOriginator) {
		return authenticateFunction.apply(requestOriginator);
	}
}
