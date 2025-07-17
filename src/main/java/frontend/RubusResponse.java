/*
 * Rubus is an application layer protocol for video and audio streaming and
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

package frontend;

import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;
import common.net.response.body.MediaInfo;
import common.net.response.body.MediaList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * RubusResponse is an auxiliary class designed to simplify response message parsing.
 */
public class RubusResponse {

	private final Logger logger = LoggerFactory.getLogger(RubusResponse.class);

	private final RubusResponseType responseType;
	private final int bodyIndex;
	private final String responseMsg;
	private final byte[] response;

	/**
	 * Constructs an instance of this class.
	 * @param response a response message represented as a byte array
	 */
	public RubusResponse(byte[] response) {
		assert response != null;

		this.response = response;
		int index = 0;
		while (index + 1 < response.length && (response[index] != '\n' | response[++index] != '\n'));

		String msg = new String(response, 0, index);
		responseType = RubusResponseType.valueOf(msg.substring(msg.indexOf(' ') + 1, msg.indexOf('\n')));
		responseMsg = msg;

		bodyIndex = index + 1;

		logger.debug("{} instantiated, response size: {}", this, response.length);
	}

	/**
	 * Returns the response type.
	 * @return the response type
	 */
	public RubusResponseType getResponseType() {
		return responseType;
	}

	/**
	 * Returns the body part of the response message to the LIST request message represented as {@link MediaList}.
	 * @return a {@link MediaList} instance
	 */
	public MediaList LIST() {
		String serializedObjName = responseMsg.substring(
			responseMsg.indexOf("serialized-object ") + "serialized-object ".length(),
			responseMsg.indexOf('\n', responseMsg.indexOf("serialized-object "))
		);
		if (serializedObjName.equals(MediaList.class.getName())) {
			try {
				return (MediaList) new ObjectInputStream(new ByteArrayInputStream(response, bodyIndex, response.length - bodyIndex)).readObject();
			} catch (IOException | ClassNotFoundException ignored) {}
		}
		return null;
	}

	/**
	 * Returns the body part of the response message to the INFO request message represented as {@link MediaInfo}.
	 * @return a {@link MediaInfo} instance
	 */
	public MediaInfo INFO() {
		String serializedObjName = responseMsg.substring(
			responseMsg.indexOf("serialized-object ") + "serialized-object ".length(),
			responseMsg.indexOf('\n', responseMsg.indexOf("serialized-object "))
		);
		if (serializedObjName.equals(MediaInfo.class.getName())) {
			try {
				return (MediaInfo) new ObjectInputStream(new ByteArrayInputStream(response, bodyIndex, response.length - bodyIndex)).readObject();
			} catch (IOException | ClassNotFoundException ignored) {}
		}
		return null;
	}

	/**
	 * Returns the body part of the response message to the FETCH request message represented as {@link FetchedPieces}.
	 * @return a {@link FetchedPieces} instance
	 */
	public FetchedPieces FETCH() {
		String serializedObjName = responseMsg.substring(
			responseMsg.indexOf("serialized-object ") + "serialized-object ".length(),
			responseMsg.indexOf('\n', responseMsg.indexOf("serialized-object "))
		);
		if (serializedObjName.equals(FetchedPieces.class.getName())) {
			try {
				return (FetchedPieces) new ObjectInputStream(new ByteArrayInputStream(response, bodyIndex, response.length - bodyIndex)).readObject();
			} catch (IOException | ClassNotFoundException ignored) {}
		}
		return null;
	}
}
