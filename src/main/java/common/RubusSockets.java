/*
 * Rubus is an application level protocol for video and audio streaming and
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

package common;

import backend.RubusServerSocket;
import backend.TCPRubusServerSocket;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;

public class RubusSockets {
	private static Class<? extends RubusSocket> rubusSocketClass = TCPRubusSocket.class;

	private static Class<? extends RubusServerSocket> rubusServerSocketClass = TCPRubusServerSocket.class;

	public static RubusSocket getRubusSocket(InetAddress inetAddress, int port) throws IOException {
		try {
			return rubusSocketClass.getConstructor(InetAddress.class, Integer.class).newInstance(inetAddress, port);
		} catch (NoSuchMethodException noSuchMethodException) {
			throw new RuntimeException("The class lacks the default constructor", noSuchMethodException);
		} catch (InvocationTargetException invocationTargetException) {
			throw new IOException(invocationTargetException);
		} catch (ReflectiveOperationException cause) {
			throw new RuntimeException(cause);
		}
	}

	public static RubusServerSocket getRubusServerSocket(int port) throws IOException {
		try {
			return rubusServerSocketClass.getConstructor(Integer.class).newInstance(port);
		} catch (NoSuchMethodException noSuchMethodException) {
			throw new RuntimeException("The class lacks the default constructor", noSuchMethodException);
		} catch (InvocationTargetException invocationTargetException) {
			throw new IOException(invocationTargetException);
		} catch (ReflectiveOperationException cause) {
			throw new RuntimeException(cause);
		}
	}

	public static void setRubusSocketClass(Class<? extends RubusSocket> rubusSocketClass) {
		RubusSockets.rubusSocketClass = rubusSocketClass;
	}

	public static void setRubusServerSocketClass(Class<? extends RubusServerSocket> rubusServerSocketClass) {
		RubusSockets.rubusServerSocketClass = rubusServerSocketClass;
	}
}
