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

import backend.TCPRubusServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TCPRubusSocketTests extends RubusSocketTests {

	RubusSocket socket;

	RubusSocket peerSocket;

	static TCPRubusServerSocket serverSocket;

	@BeforeAll
	static void beforeAll() {
		assertDoesNotThrow(() -> serverSocket = new TCPRubusServerSocket(55000));
	}

	@BeforeEach
	void beforeEach() {
		Thread thread = new Thread(() ->
			assertDoesNotThrow( () -> {
				peerSocket = serverSocket.accept();
			})
		);
		thread.start();
		assertDoesNotThrow(() -> socket = new TCPRubusSocket(InetAddress.getByName("localhost"), 55000));
	}

	@AfterAll
	static void afterAll() {
		assertDoesNotThrow(() -> serverSocket.close());
	}

	@Override
	protected RubusSocket getSocket() {
		return socket;
	}

	@Override
	protected RubusSocket getPeerSocket() {
		return peerSocket;
	}

	@AfterEach
	void afterEach() {
		assertDoesNotThrow(() -> {
			socket.close();
			peerSocket.close();
		});
	}
}
