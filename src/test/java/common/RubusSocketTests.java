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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public abstract class RubusSocketTests {

	protected abstract RubusSocket getSocket();

	protected abstract RubusSocket getPeerSocket();

	@Test
	void closeTest() throws IOException {
		RubusSocket socket = getSocket();
		socket.close();
		assertTrue(socket.isClosed());
	}

	@Test
	void openAndCloseTime() throws IOException {
		RubusSocket socket = getSocket();
		socket.close();
		assertTrue(socket.openedTime() > 0, "The open time is not set");
		assertTrue(
			socket.closedTime() >= socket.openedTime(),
			"The close time is not greater than the open time"
		);
	}

	@Test
	void sendingAndReceiving() throws IOException{
		byte[] byteArray = new byte[1000];
		for (int i = 0; i < byteArray.length; i++) {
			byteArray[i] = (byte) i;
		}
		RubusSocket socket = getSocket();
		socket.write(byteArray);
		RubusSocket peer = getPeerSocket();
		byte[] receiveBuffer = new byte[byteArray.length];
		int byteRead = 0;
		do {
			byteRead += peer.read(receiveBuffer, byteRead, receiveBuffer.length - byteRead);
		} while (byteRead < receiveBuffer.length);

		assertArrayEquals(byteArray, receiveBuffer, "The sent array is different from the receivedArray");
	}

	@Test
	void sendingAndReceivingAsync() throws IOException {
		byte[] byteArray = new byte[100000];
		for (int i = 0; i < byteArray.length; i++) {
			byteArray[i] = (byte) i;
		}

		new Thread(() -> assertDoesNotThrow(() -> {
			RubusSocket socket = getSocket();
			socket.write(byteArray);
		}, "Async sending failed")).start();

		RubusSocket peer = getPeerSocket();
		byte[] receiveBuffer = new byte[byteArray.length];
		int byteRead = 0;
		do {
			byteRead += peer.read(receiveBuffer, byteRead, receiveBuffer.length - byteRead);
		} while (byteRead < receiveBuffer.length);

		assertArrayEquals(byteArray, receiveBuffer, "The sent array is different from the receivedArray");
	}

	@Test
	void receivingTimeoutTest() {
		assertThrows(IOException.class, () -> {
			RubusSocket socket = getSocket();
			socket.read(new byte[1], 500);
		});
	}

	@Test
	void sendAndReceiveTime() throws IOException {
		byte[] byteArray = new byte[1000];
		for (int i = 0; i < byteArray.length; i++) {
			byteArray[i] = (byte) i;
		}
		RubusSocket socket = getSocket();
		long sTime0 = socket.lastSentTime();
		RubusSocket peer = getPeerSocket();
		long rTime0 = socket.lastReceivedTime();

		socket.write(byteArray);
		byte[] receiveBuffer = new byte[byteArray.length];
		int byteRead = 0;
		do {
			byteRead += peer.read(receiveBuffer, byteRead, receiveBuffer.length - byteRead);
		} while (byteRead < receiveBuffer.length);
		long sTime1 = socket.lastSentTime();
		long rTime1 = peer.lastReceivedTime();
		assertTrue(sTime1 >= sTime0, "The first send time is less than the initial send time");
		assertTrue(
			rTime1 >= rTime0,
			"The first receive time is less than the initial receive time"
		);

		socket.write(byteArray);
		byteRead = 0;
		do {
			byteRead += peer.read(receiveBuffer, byteRead, receiveBuffer.length - byteRead);
		} while (byteRead < receiveBuffer.length);
		assertTrue(
			socket.lastSentTime() >= sTime1,
			"The second send time is less than the first send time"
		);
		assertTrue(
			peer.lastReceivedTime() >= rTime1,
			"The second receive time less greater than the receive send time"
		);
	}
}
