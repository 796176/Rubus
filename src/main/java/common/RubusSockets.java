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

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Arrays;

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


	public static byte[] extractMessage(RubusSocket socket, long timeout) throws IOException {
		int maxHeaderAllocation = 1024 * 8;
		byte[] response = new byte[1024];
		int emptyLineIndex;
		int byteReadTotal = 0;
		while ((emptyLineIndex = searchSubArray(response, "\n\n".getBytes())) == -1) {
			if (byteReadTotal > maxHeaderAllocation) throw new IllegalArgumentException();
			if (byteReadTotal == response.length)
				response = Arrays.copyOf(response, response.length * 2);
			long time = System.currentTimeMillis();
			int byteRead = socket.read(response, timeout);
			if (timeout != 0) timeout = Math.max(timeout - (System.currentTimeMillis() - time), 1);
			if (byteRead == -1) throw new EOFException();
			byteReadTotal += byteRead;
		}

		String header = new String(response, 0, emptyLineIndex + 1);
		int bodyLen = Integer.parseInt(header.substring(
			header.indexOf("body-length ") + "body-length ".length(),
			header.indexOf('\n', header.indexOf("body-length "))
		));

		response = Arrays.copyOf(response, header.length() + "\n".length() + bodyLen);
		do {
			int remaining = response.length - byteReadTotal;
			long time = System.currentTimeMillis();
			byteReadTotal += socket.read(response, byteReadTotal, remaining, timeout);
			if (timeout != 0) timeout = Math.max(timeout - (System.currentTimeMillis() - time), 1);
		} while (byteReadTotal < response.length);
		return response;
	}


	private static int searchSubArray(byte[] oArr, byte[] sArr) {
		int sArrayByteIndex = 0;
		for (int i = 0; i < oArr.length - sArr.length + 1; i++) {
			if (sArrayByteIndex == sArr.length) return i - sArr.length;
			if (oArr[i] == sArr[sArrayByteIndex]) sArrayByteIndex++;
			else sArrayByteIndex = 0;
		}
		return -1;
	}}
