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

package frontend.interactors;

/**
 * ExceptionHandler is an alternative to Java's try/catch statement. It is a functional interface similar to
 * {@link java.util.function.Consumer}, but the argument type is limited to {@link Exception} and its subclasses.<br>
 * Java's try/catch statement doesn't fit when the exception needs to be handled by a non-callee. If the method A
 * invokes the method B, the B's exceptions need to be either handled by A or passed further up the invocation stack.
 * With ExceptionHandler, however, the B's exceptions can be handled by the object C that implements this functional
 * interface.<br>
 * This is the handler participant of the Chain-of-responsibility pattern.
 */
public interface ExceptionHandler {

	/**
	 * Handles the passed exception.
	 * @param e the exception to handle
	 */
	void handleException(Exception e);
}
