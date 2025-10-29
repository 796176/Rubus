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

package frontend.exceptions;

/**
 * This exception is thrown when {@link frontend.decoders.Decoder} fails to decode.
 */
public class DecodingException extends RuntimeException {

	/**
	 * <strong>Description copied from class: {@link Throwable}</strong><br>
	 * Constructs a new exception with {@code null} as its detail message.
	 * The cause is not initialized, and may subsequently be initialized by a
	 * call to {@link #initCause}.
	 */
	public DecodingException() { }
	/**
	 * <strong>Description copied from class: {@link Throwable}</strong><br>
	 * Constructs a new exception with the specified detail message. The
	 * cause is not initialized, and may subsequently be initialized by
	 * a call to {@link #initCause}.
	 *
	 * @param message the detail message. The detail message is saved for
	 *                later retrieval by the {@link #getMessage()} method.
	 */
	public DecodingException(String message) {
		super(message);
	}

	/**
	 * <strong>Description copied from class: {@link Throwable}</strong><br>
	 * Constructs a new exception with the specified detail message and
	 * cause.
	 * <p>Note that the detail message associated with
	 * {@code cause} is <i>not</i> automatically incorporated in
	 * this exception's detail message.
	 *
	 * @param message the detail message (which is saved for later retrieval
	 *                by the {@link #getMessage()} method).
	 * @param cause the cause (which is saved for later retrieval by the
	 *              {@link #getCause()} method). (A {@code null} value is
	 *              permitted, and indicates that the cause is nonexistent or
	 *              unknown.)
	 */
	public DecodingException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * <strong>Description copied from class: {@link Throwable}</strong><br>
	 * Constructs a new exception with the specified cause and a detail
	 * message of {@code (cause==null ? null : cause.toString())} (which
	 * typically contains the class and detail message of {@code cause}).
	 * This constructor is useful for exceptions that are little more than
	 * wrappers for other throwables (for example, {@link
	 * java.security.PrivilegedActionException}).
	 *
	 * @param cause the cause (which is saved for later retrieval by the
	 *              {@link #getCause()} method). (A {@code null} value is
	 *              permitted, and indicates that the cause is nonexistent or
	 *              unknown.)
	 */
	public DecodingException(Throwable cause) {
		super(cause);
	}
}
