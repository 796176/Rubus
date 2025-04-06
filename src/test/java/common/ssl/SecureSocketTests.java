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

package common.ssl;

import backend.TCPRubusServerSocket;
import common.Config;
import common.RubusSocket;
import common.RubusSocketTests;
import common.TCPRubusSocket;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SecureSocketTests extends RubusSocketTests {

	static TCPRubusServerSocket serverSocket;

	static String localResourcesDir =
		Path.of(System.getProperty("user.dir"), "src", "test", "resources", "ssl_data").toString();

	RubusSocket socket;

	RubusSocket peerSocket;

	Config serverConfig;

	Config clientConfig;

	@BeforeAll
	static void beforeAll() throws IOException {
		serverSocket = new TCPRubusServerSocket(InetAddress.getByName("localhost"), 55000);
	}

	@BeforeEach
	void beforeEach() throws InterruptedException, IOException{
		clientConfig = new Config(Path.of(localResourcesDir, "test_client.conf"));
		serverConfig = new Config(Path.of(localResourcesDir, "test_server.conf"));
		serverConfig.set("private-key-location", Path.of(localResourcesDir, "test_server.key").toString());
		serverConfig.set("certificate-location", Path.of(localResourcesDir, "test_server.crt").toString());

		Thread peerThread = new Thread(() ->
			assertDoesNotThrow(() ->
				peerSocket = new SecureSocket(serverSocket.accept(), serverConfig, false)
			)
		);
		peerThread.start();

		assertDoesNotThrow(() -> socket = new SecureSocket(
			new TCPRubusSocket(InetAddress.getByName("localhost"), 55000), clientConfig, true
		));
		peerThread.join();
	}

	@AfterEach
	void afterEach() {
		assertDoesNotThrow(() -> {
			socket.close();
			peerSocket.close();
		});
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

	static Stream<byte[]> testArrayProvider() {
		byte[] smallArray = new byte[] {0};
		byte[] bigArray = new byte[50];
		for (int i = 0; i < bigArray.length; i++) {
			bigArray[i] = (byte) i;
		}

		return Stream.of(smallArray, bigArray);
	}

	@ParameterizedTest
	@MethodSource("testArrayProvider")
	void encryptionTest(byte[] payload) throws IOException, ReflectiveOperationException, GeneralSecurityException {
		Field socketField = SecureSocket.class.getDeclaredField("socket");
		socketField.setAccessible(true);
		RubusSocket underlyingSocket = (RubusSocket) socketField.get(peerSocket);
		Field keyField = SecureSocket.class.getDeclaredField("sKey");
		keyField.setAccessible(true);
		Key key = (Key) keyField.get(peerSocket);
		Field macField = SecureSocket.class.getDeclaredField("mac");
		macField.setAccessible(true);
		Mac mac = (Mac) macField.get(peerSocket);
		// resetting mac just in case
		mac.doFinal();
		Field cipherField = SecureSocket.class.getDeclaredField("cipher");
		cipherField.setAccessible(true);
		Cipher cipher = (Cipher) cipherField.get(peerSocket);

		socket.write(payload);
		byte[] mes = new byte[4];
		int totalRead = underlyingSocket.read(mes);
		int mesLength = mes[0] << 24 | mes[1] << 16 | mes[2] << 8 | mes[3];
		mes = Arrays.copyOf(mes, mesLength);
		while (totalRead < mesLength) {
			totalRead += underlyingSocket.read(mes, totalRead, mesLength - totalRead);
		}

		IvParameterSpec ivSpec = new IvParameterSpec(mes, 4, 16);
		byte[] macResult = mac.doFinal(payload);
		cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
		byte[] signedPayload = new byte[payload.length + macResult.length];
		System.arraycopy(payload, 0, signedPayload, 0, payload.length);
		System.arraycopy(macResult, 0, signedPayload, payload.length, macResult.length);
		byte[] encryptedPayload = cipher.doFinal(signedPayload);
		int encryptedMesLength = 4 + ivSpec.getIV().length + encryptedPayload.length;
		byte[] encryptedMes = new byte[encryptedMesLength];
		encryptedMes[0] = (byte) (encryptedMesLength >> 24 & 0xFF);
		encryptedMes[1] = (byte) (encryptedMesLength >> 16 & 0xFF);
		encryptedMes[2] = (byte) (encryptedMesLength >>  8 & 0xFF);
		encryptedMes[3] = (byte) (encryptedMesLength >>  0 & 0xFF);
		System.arraycopy(ivSpec.getIV(), 0, encryptedMes, 4, ivSpec.getIV().length);
		System.arraycopy(encryptedPayload, 0, encryptedMes, 4 + ivSpec.getIV().length, encryptedPayload.length);
		assertArrayEquals(encryptedMes, mes);
	}

	@Nested
	class FailHandshake {

		@Test
		void clientDoesNotSupportSC() throws InterruptedException, IOException {
			clientConfig.set("secure-connection-enabled", "false");
			Thread serverThread = new Thread(() -> {
				try {
					RubusSocket localPeerSocket = serverSocket.accept();
					assertThrowsExactly(
						HandshakeFailedException.class,
						() -> new SecureSocket(localPeerSocket, serverConfig, false),
						"Server didn't fail while attempting to establish a secure connection");
					localPeerSocket.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			serverThread.start();

			RubusSocket localSocket = new TCPRubusSocket(InetAddress.getByName("localhost"), 55000);
			assertThrowsExactly(
				HandshakeFailedException.class,
				() -> new SecureSocket(localSocket, clientConfig, true),
				"Client didn't fail while attempting to establish a secure connection");
			localSocket.close();
			serverThread.join();
		}

		@Test
		void serverDoesNotSupportSC() throws InterruptedException, IOException {
			serverConfig.set("secure-connection-enabled", "false");
			Thread serverThread = new Thread(() -> {
				try {
					RubusSocket localPeerSocket = serverSocket.accept();
					assertThrowsExactly(
						HandshakeFailedException.class,
						() -> new SecureSocket(localPeerSocket, serverConfig, false),
						"Server didn't fail while attempting to establish a secure connection"
					);
					localPeerSocket.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			serverThread.start();

			RubusSocket localSocket = new TCPRubusSocket(InetAddress.getByName("localhost"), 55000);
			assertThrowsExactly(
				HandshakeFailedException.class,
				() -> new SecureSocket(
					localSocket,
					clientConfig,
					true
				),
				"Client didn't fail while attempting to establish a secure connection"
			);
			localSocket.close();
			serverThread.join();
		}

		@Test
		void clientAndServerDoNotSupportSC() throws InterruptedException, IOException{
			serverConfig.set("secure-connection-enabled", "false");
			clientConfig.set("secure-connection-enabled", "false");
			Thread serverThread = new Thread(() -> {
				try {
					RubusSocket localPeerSocket = serverSocket.accept();
					assertThrowsExactly(
						HandshakeFailedException.class,
						() -> new SecureSocket(localPeerSocket, serverConfig, false),
						"Server didn't fail while attempting to establish a secure connection"
					);
					localPeerSocket.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			serverThread.start();

			RubusSocket localSocket = new TCPRubusSocket(InetAddress.getByName("localhost"), 55000);
			assertThrowsExactly(
				HandshakeFailedException.class,
				() -> new SecureSocket(localSocket, clientConfig, true),
				"Client didn't fail while attempting to establish a secure connection"
			);
			localSocket.close();
			serverThread.join();
		}
	}
}
