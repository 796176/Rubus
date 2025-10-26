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

import backend.authorization.ActionType;
import backend.authorization.ViewerAuthorizer;
import backend.exceptions.NotImplementedExceptions;
import backend.models.Viewer;
import jakarta.annotation.Nonnull;

import java.util.function.BiFunction;

public class ViewerAuthorizerStub implements ViewerAuthorizer {

	public BiFunction<Viewer, ActionType, Boolean> validateFunction = (v, type) -> {
		throw new NotImplementedExceptions();
	};

	@Override
	public boolean validate(@Nonnull Viewer viewer, @Nonnull ActionType type) {
		return validateFunction.apply(viewer, type);
	}
}
