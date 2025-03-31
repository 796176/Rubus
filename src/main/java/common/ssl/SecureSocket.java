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

import common.Config;
import common.RubusSocket;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.*;

/**
 * SecureSocket class provides transparent encryption of all the data it sends as well as transparent decryption of
 * all the data it receives. The decorator pattern is the core design of this class allowing it to use any instance of
 * {@link RubusSocket} as its underlying socket. The secure connection is established by performing a handshake where
 * 2 hosts exchange the necessary data to generate a symmetric key they use to encrypt and decrypt payloads and a key to
 * generate MAC for payload verification. After the handshake is performed 2 hosts can securely exchange messages
 * between each other.<br><br>
 *
 *
 * The message format of SecureSocket:
 * <pre>
 *  _____________________________________________________________________________________
 * |                         |                      |                                    |
 * | The size of the message |  The initial vector  |      The encrypted payload         |
 * |        4 bytes          |        16 bytes      |                                    |
 * |_________________________|______________________|____________________________________|
 *                                                  /                                    |
 *                                                 /                                     |
 *                                                /                                      |
 *                                               /                                       |
 *                                              /                                        |
 *                                             /_________________________________________|
 *                                             |                           |             |
 *                                             |     The non-encrypted     |     MAC     |
 *                                             |          payload          |   32 bytes  |
 *                                             |___________________________|_____________|
 * </pre>
 *
 * Note: the current implementation contains severe security issues:<br>
 * 1. The authenticity of the certificate isn't checked by the client socket<br>
 * 2. The mac uses the outdated hashing protocol<br>
 * 3. The client and the server use the same key to decrypt messages they send to each other<br>
 * 4. The client and the server use the same key to compute mac results<br>
 * 5. The message format doesn't support sequential numbers<br>
 * 6. The integrity of the handshake isn't checked<br>
 * 7. There is no way to generate a new symmetric key without closing the current sockets<br>
 * 8. Hello message don't use nonce making it vulnerable to replay attacks<br>
 * Improving the current implementation and switching from javax.crypto to modern cryptography libraries is on
 * the TODO list but it's not a priority.
 */
public class SecureSocket implements RubusSocket {

	private final static int LENGTH_FIELD_SIZE = 4;

	private final static int IV_LENGTH = 16;

	private final static int NONCE_LENGTH = 64;

	private final static int KEY_OFFSET = 0;

	private final static int KEY_LENGTH = 32;

	private final static int MAC_KEY_OFFSET = 32;

	private final static int MAC_KEY_LENGTH = 32;

	private final RubusSocket socket;

	private final long oTime;

	private long sTime = 0;

	private long rTime = 0;

	private Key sKey;

	private Mac mac;

	private byte[] remainder = new byte[0];

	private int remainderLength = 0;

	private int remainderOffset = 0;

	private final Cipher cipher;

	private final boolean encryptionSupported;

	/**
	 * Constructs an instance of this class and performs the handshake. If this socket is a handshake initiator, the
	 * handshake starts by sending the hello message. If this socket is not a handshake initiator, it waits to receive
	 * the hello message. If the handshake isn't done within the specified time, the constructor throws
	 * {@link InterruptedException} and closes the socket.<br>
	 * During the construction, this SecureSocket looks for the "encryption-enabled" value in the config. If this socket
	 * doesn't initiate a handshake it also looks for the "certificate-location" and "private-key-location" values.<br>
	 * If this socket or/and the peer socket don't support a secure connection, the construction fails by throwing
	 * {@link HandshakeFailedException}.
	 * @param socket the concrete socket
	 * @param config the config
	 * @param handshakeTimeout the timeout in milliseconds to establish a secure connection, or 0 to wait indefinitely
	 * @param handshakeInitiator specifies if this socket is a handshake initiator
	 * @throws IOException if some I/O error occurs
	 * @throws SocketTimeoutException if the secure connection wasn't established within the specified time
	 * @throws InterruptedException if the handshake was interrupted
	 * @throws HandshakeFailedException if this socket or/and the peer socket don't support secure connection
	 * @throws CorruptedSSLMessageException if some handshake massages were tampered
	 */
	public SecureSocket(
		RubusSocket socket, Config config, long handshakeTimeout, boolean handshakeInitiator
	) throws IOException, InterruptedException {
		assert socket != null && config != null && handshakeTimeout >= 0;

		encryptionSupported = Boolean.parseBoolean(config.get("encryption-enabled"));

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<?> future = executorService.submit(() -> {
			if (handshakeInitiator) performClientHandshake(socket);
			else performServerHandshake(socket, config);
			return null;
		});
		executorService.shutdown();
		boolean isTerminated = executorService
			.awaitTermination(handshakeTimeout > 0 ? handshakeTimeout : Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		if (!isTerminated) {
			socket.close();
			throw new SocketTimeoutException();
		}
		if (future.state() == Future.State.FAILED) {
			Throwable exception = future.exceptionNow();
			if (exception instanceof IOException ioException) throw ioException;
			else if (exception instanceof RuntimeException runtimeException) throw runtimeException;
		}
		oTime = System.currentTimeMillis();

		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		this.socket = socket;
	}

	/**
	 * Constructs an instance of this class and performs the handshake. If this socket is a handshake initiator, the
	 * handshake starts by sending the hello message. If this socket is not a handshake initiator, it waits to receive
	 * the hello message. Both peers wait indefinitely to establish a secure connection<br>
	 * During the construction this SecureSocket looks for the "encryption-enabled" value in the config. If this socket
	 * doesn't initiate a handshake it also looks for the "certificate-location" and "private-key-location" values.<br>
	 * If this socket or/and the peer socket don't support a secure connection, the construction fails by throwing
	 * {@link HandshakeFailedException}.
	 * @param socket the concrete socket
	 * @param config the config
	 * @param handshakeInitiator specifies if this socket is the handshake initiator
	 * @throws IOException if some I/O error occurs
	 * @throws SocketTimeoutException if the secure connection wasn't established within the specified time
	 * @throws InterruptedException if the handshake was interrupted
	 * @throws HandshakeFailedException if this socket or/and the peer socket don't support secure connection
	 * @throws CorruptedSSLMessageException if some handshake massages were tampered
	 */
	public SecureSocket(
		RubusSocket socket, Config config, boolean handshakeInitiator
	) throws IOException, InterruptedException {
		this(socket, config, 0, handshakeInitiator);
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}

	@Override
	public void close(long timeout) throws IOException {
		socket.close(timeout);
	}

	/**
	 * Receives the message containing the encrypted payload using the underlying socket, decrypts and verifies it, and
	 * writes the decrypted payload into the given array. If the given array isn't large enough to fit the entire
	 * payload, the part that wasn't written is cached and will be written during the next invocation of any read methods.
	 * @param in the array data is written into.
	 * @return the number of bytes written
	 * @throws IOException if some I/O error occurs
	 * @throws CorruptedSSLMessageException if the message was corrupted
	 */
	@Override
	public int read(byte[] in) throws IOException {
		return read(in, 0, in.length, 0);
	}

	/**
	 * Receives the message containing the encrypted payload using the underlying socket, decrypts and verifies it, and
	 * writes the decrypted payload into the given array. If the given array isn't large enough to fit the entire
	 * payload, the part that wasn't written is cached and will be written during the next invocation of any read method.
	 * @param in the array data is written into
	 * @param timeout the timeout in milliseconds
	 * @return the number of bytes written
	 * @throws IOException if some I/O error occurs
	 * @throws CorruptedSSLMessageException if the message was corrupted
	 */
	@Override
	public int read(byte[] in, long timeout) throws IOException {
		return read(in, 0, in.length, timeout);
	}

	/**
	 * Receives the message containing the encrypted payload using the underlying socket, decrypts and verifies it, and
	 * writes the decrypted payload into the given array. If the range of the given array isn't large enough to fit
	 * the entire payload, the part that wasn't written is cached and will be written during the next invocation of
	 * any read method.
	 * @param in the array data is written into
	 * @param offset the index of the array the first byte is written into
	 * @param length the length of the available array
	 * @return the number of bytes written
	 * @throws IOException if some I/O error occurs
	 * @throws CorruptedSSLMessageException if the message was corrupted
	 */
	@Override
	public int read(byte[] in, int offset, int length) throws IOException {
		return read(in, offset, length, 0);
	}

	/**
	 * Receives the message containing the encrypted payload using the underlying socket, decrypts and verifies it, and
	 * writes the decrypted payload into the given array. If the range of the given array isn't large enough to fit
	 * the entire payload, the part that wasn't written is cached and will be written during the next invocation of
	 * any read method.
	 * @param in the array data is written into
	 * @param offset the index of the array the first byte is written into
	 * @param length the length of the available array
	 * @param timeout the timeout in milliseconds
	 * @return the number of bytes written
	 * @throws IOException if some I/O error occurs
	 * @throws CorruptedSSLMessageException if the message was corrupted
	 */
	@Override
	public int read(byte[] in, int offset, int length, long timeout) throws IOException {
		assert offset >= 0 && length >= 0 && offset + length <= in.length && timeout >= 0;

		if (length == 0) return 0;
		if (remainderLength > remainderOffset) {
			int byteCopied = Math.min(length, remainderLength - remainderOffset);
			System.arraycopy(remainder, remainderOffset, in, offset, byteCopied);
			remainderOffset += byteCopied;
			return byteCopied;
		}
		try {
			byte[] encryptedMes = readMessage(socket, timeout);

			IvParameterSpec ivParameterSpec = new IvParameterSpec(encryptedMes, LENGTH_FIELD_SIZE, IV_LENGTH);
			cipher.init(Cipher.DECRYPT_MODE, sKey, ivParameterSpec);
			byte[] signedPayload =
				cipher.doFinal(
					encryptedMes,
					LENGTH_FIELD_SIZE + IV_LENGTH,
					encryptedMes.length - LENGTH_FIELD_SIZE - IV_LENGTH
				);
			int originalMesLength = signedPayload.length - mac.getMacLength();
			mac.update(signedPayload, 0, originalMesLength);
			byte[] macResult = mac.doFinal();
			boolean macResultMatches = Arrays
				.equals(
					macResult, 0, mac.getMacLength(),
					signedPayload, originalMesLength, signedPayload.length
				);
			if (!macResultMatches) throw new CorruptedSSLMessageException("MAC result doesn't match");

			int byteCopied = Math.min(originalMesLength, length);
			System.arraycopy(signedPayload, 0, in, offset, byteCopied);
			if (byteCopied < originalMesLength) {
				remainder = signedPayload;
				remainderOffset = byteCopied;
				remainderLength = originalMesLength;
			}
			rTime = System.currentTimeMillis();
			return byteCopied;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Encrypts the given array and sends it using the underlying socket.
	 * @param out data to send
	 * @throws IOException if some I/O error occurs
	 */
	@Override
	public void write(byte[] out) throws IOException {
		write(out, 0, out.length);
	}

	/**
	 * Encrypts the range of the given array and sends it using the underlying socket.
	 * @param out the data to send
	 * @param offset the index of the first byte to send
	 * @param length the number of bytes to send
	 * @throws IOException if some I/O error occurs
	 */
	@Override
	public void write(byte[] out, int offset, int length) throws IOException {
		try {
			mac.update(out, offset, length);
			byte[] macResult = mac.doFinal();
			cipher.init(Cipher.ENCRYPT_MODE, sKey);
			byte[] encryptedPayload1 = cipher.update(out, offset, length);
			byte[] encryptedPayload2 = cipher.doFinal(macResult);
			byte[] iv = cipher.getIV();
			sendMessage(socket, iv, encryptedPayload1, encryptedPayload2);
			sTime = System.currentTimeMillis();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isClosed() {
		return socket.isClosed();
	}

	/**
	 * Returns the time in milliseconds when the secure connection was established.
	 * @return the time in milliseconds when the secure connection was established
	 */
	@Override
	public long openTime() {
		return oTime;
	}

	@Override
	public long closeTime() {
		return socket.closeTime();
	}

	@Override
	public long lastReceiveTime() {
		return rTime;
	}

	@Override
	public long lastSendTime() {
		return sTime;
	}

	private void performClientHandshake(RubusSocket socket) throws IOException {
		try {
			String helloMes = """
					hello
					encryption-support:%b
					""".formatted(encryptionSupported);
			sendMessage(socket, helloMes.getBytes());

			byte[] helloResponse = readMessage(socket, 0);
			if (new String(helloResponse).toLowerCase().contains("encryption-support:false") || !encryptionSupported) {
				throw new HandshakeFailedException("This or the peer socket don't support a secure connection");
			}
			ByteArrayInputStream cert =
				new ByteArrayInputStream(
					helloResponse,
					LENGTH_FIELD_SIZE,
					helloResponse.length - LENGTH_FIELD_SIZE
				);
			Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(cert);
			Key publicKey = certificate.getPublicKey();
			SecureRandom secureRandom = new SecureRandom();
			byte[] nonce = new byte[NONCE_LENGTH];
			secureRandom.nextBytes(nonce);
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] encryptedNonce = cipher.doFinal(nonce);
			sendMessage(socket, encryptedNonce);

			sKey = new SecretKeySpec(nonce, KEY_OFFSET, KEY_LENGTH, "AES");
			Key macKey = new SecretKeySpec(nonce, MAC_KEY_OFFSET, MAC_KEY_LENGTH, "AES");
			mac = Mac.getInstance("HmacSHA256");
			mac.init(macKey);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private void performServerHandshake(RubusSocket socket, Config config) throws IOException {
		try {
			byte[] helloMes = readMessage(socket, 0);

			if (new String(helloMes).toLowerCase().contains("encryption-support:false") || !encryptionSupported) {
				String helloResponse = """
						hello
						encryption-support:%b
						""".formatted(encryptionSupported);
				sendMessage(socket, helloResponse.getBytes());
				throw new HandshakeFailedException("This or the peer socket doesn't support secure connection");
			}

			Path certificatePath = Path.of(config.get("certificate-location"));
			byte[] certificate = Files.readAllBytes(certificatePath);
			sendMessage(socket, certificate);

			byte[] encryptedNonceMes = readMessage(socket, 0);
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			Path privateKeyPath = Path.of(config.get("private-key-location"));
			cipher.init(Cipher.DECRYPT_MODE, extractPrivateKey(privateKeyPath));
			byte[] nonce =
				cipher.doFinal(
					encryptedNonceMes,
					LENGTH_FIELD_SIZE,
					encryptedNonceMes.length - LENGTH_FIELD_SIZE
				);
			if (nonce.length != NONCE_LENGTH) {
				throw new CorruptedSSLMessageException("Nonce length isn't equal to " + NONCE_LENGTH);
			}

			sKey = new SecretKeySpec(nonce, KEY_OFFSET, KEY_LENGTH, "AES");
			Key macKey = new SecretKeySpec(nonce, MAC_KEY_OFFSET, MAC_KEY_LENGTH, "AES");
			mac = Mac.getInstance("HmacSHA256");
			mac.init(macKey);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] convertLength(int i) {
		byte[] arr = new byte[4];
		arr[0] = (byte) (i >> 24 & 0xFF);
		arr[1] = (byte) (i >> 16 & 0xFF);
		arr[2] = (byte) (i >>  8 & 0xFF);
		arr[3] = (byte) (i >>  0 & 0xFF);
		return arr;
	}

	private int extractLength(byte[] bytes) {
		int integer = 0;
		integer = integer | (bytes[0] << 24 & 0xFF000000);
		integer = integer | (bytes[1] << 16 & 0x00FF0000);
		integer = integer | (bytes[2] <<  8 & 0x0000FF00);
		integer = integer | (bytes[3] <<  0 & 0x000000FF);
		return integer;
	}

	private PrivateKey extractPrivateKey(Path pKeyPath) throws IOException {
		assert pKeyPath != null;

		String pemHeader = "-----BEGIN PRIVATE KEY-----";
		String pemFooter = "-----END PRIVATE KEY-----";
		try {
			String pKeyPem = Files.readString(pKeyPath);
			String pKeyPemParsed = pKeyPem
				.substring(
					pKeyPem.indexOf(pemHeader) + pemHeader.length(),
					pKeyPem.indexOf(pemFooter)
				)
				.replace("\n", "");
			byte[] pKeyPemDecoded = Base64.getDecoder().decode(pKeyPemParsed);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pKeyPemDecoded);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePrivate(keySpec);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] readMessage(RubusSocket socket, long timeout) throws IOException {
		long readStartsTime = System.currentTimeMillis();
		byte[] mes = new byte[LENGTH_FIELD_SIZE];
		int totalRead = socket.read(mes, timeout);
		if (totalRead == -1) throw new EOFException();

		int mesLength = extractLength(mes);
		mes = Arrays.copyOf(mes, mesLength);
		while (totalRead < mesLength) {
			long timePassed = System.currentTimeMillis() - readStartsTime;
			if (timeout > 0 && timePassed >= timeout) {
				throw new SocketTimeoutException();
			}
			int byteRead =
				socket.read(
					mes,
					totalRead,
					mesLength - totalRead,
					timeout > 0 ? timeout - timePassed : 0
				);
			if (byteRead == -1) throw new EOFException();
			totalRead += byteRead;
		}
		return mes;
	}

	private void sendMessage(RubusSocket socket, byte[]... payloads) throws IOException {
		int totalLength = 0;
		for (byte[] payload: payloads) {
			if (payload != null) totalLength += payload.length;
		}
		int mesLength = LENGTH_FIELD_SIZE + totalLength;
		byte[] mes = new byte[mesLength];
		System.arraycopy(convertLength(mesLength), 0, mes, 0, LENGTH_FIELD_SIZE);
		int offset = LENGTH_FIELD_SIZE;
		for (byte[] payload: payloads) {
			if (payload != null) {
				System.arraycopy(payload, 0, mes, offset, payload.length);
				offset += payload.length;
			}
		}
		socket.write(mes);
	}
}
