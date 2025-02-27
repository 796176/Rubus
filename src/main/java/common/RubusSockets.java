/*
 * Rubus is an application level protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024-2025 Yegore Vlussove
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

package common;

import backend.RubusServerSocket;
import backend.TCPRubusServerSocket;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;

/**
 * RubusSockets contains static methods related to {@link RubusSockets}.
 */
public class RubusSockets {
	private static Class<? extends RubusSocket> rubusSocketClass = TCPRubusSocket.class;

	private static Class<? extends RubusServerSocket> rubusServerSocketClass = TCPRubusServerSocket.class;

	/**
	 * Constructs an instance of RubusSocket using the specified server address and port.
	 * @param inetAddress a server address
	 * @param port a server port
	 * @return an instance of RubusSocket
	 * @throws IOException if an {@link InvocationTargetException} exception occurs
	 */
	public static RubusSocket getRubusSocket(InetAddress inetAddress, int port) throws IOException {
		try {
			return rubusSocketClass.getConstructor(InetAddress.class, int.class).newInstance(inetAddress, port);
		} catch (NoSuchMethodException noSuchMethodException) {
			throw new RuntimeException("The class lacks the default constructor", noSuchMethodException);
		} catch (InvocationTargetException invocationTargetException) {
			throw new IOException(invocationTargetException);
		} catch (ReflectiveOperationException cause) {
			throw new RuntimeException(cause);
		}
	}

	/**
	 * Constructs an instance of RubusServerSocket using the specified listening port.
	 * @param port the listening port
	 * @return an instance of RubusServerSocket
	 * @throws IOException if an {@link InvocationTargetException} exception occurs
	 */
	public static RubusServerSocket getRubusServerSocket(int port) throws IOException {
		try {
			return rubusServerSocketClass.getConstructor(int.class).newInstance(port);
		} catch (NoSuchMethodException noSuchMethodException) {
			throw new RuntimeException("The class lacks the default constructor", noSuchMethodException);
		} catch (InvocationTargetException invocationTargetException) {
			throw new IOException(invocationTargetException);
		} catch (ReflectiveOperationException cause) {
			throw new RuntimeException(cause);
		}
	}

	/**
	 * Sets the concrete implementation that needs to be constructed by {@link #getRubusSocket(InetAddress, int)}
	 * @param rubusSocketClass the class of a concrete implementation of RubusServer
	 */
	public static void setRubusSocketClass(Class<? extends RubusSocket> rubusSocketClass) {
		RubusSockets.rubusSocketClass = rubusSocketClass;
	}

	/**
	 * Sets the concrete implementation that needs to be constructed by {@link #getRubusServerSocket(int)}
	 * @param rubusServerSocketClass the class of a concrete implementation of RubusServerSocket
	 */
	public static void setRubusServerSocketClass(Class<? extends RubusServerSocket> rubusServerSocketClass) {
		RubusSockets.rubusServerSocketClass = rubusServerSocketClass;
	}
}
