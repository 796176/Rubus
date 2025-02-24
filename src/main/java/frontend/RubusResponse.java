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

package frontend;

import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;
import common.net.response.body.PlaybackInfo;
import common.net.response.body.PlaybackList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class RubusResponse {

	private final RubusResponseType responseType;
	private final int bodyIndex;
	private final String responseMsg;
	private final byte[] response;

	public RubusResponse(byte[] response) {
		assert response != null;

		this.response = response;
		int index = 0;
		while (index + 1 < response.length && (response[index] != '\n' | response[++index] != '\n'));

		String msg = new String(response, 0, index);
		responseType = RubusResponseType.valueOf(msg.substring(msg.indexOf(' ') + 1, msg.indexOf('\n')));
		responseMsg = msg;

		bodyIndex = index + 1;
	}

	public RubusResponseType getResponseType() {
		return responseType;
	}

	public PlaybackList LIST() {
		String serializedObjName = responseMsg.substring(
			responseMsg.indexOf("serialized-object ") + "serialized-object ".length(),
			responseMsg.indexOf('\n', responseMsg.indexOf("serialized-object "))
		);
		if (serializedObjName.equals(PlaybackList.class.getName())) {
			try {
				return (PlaybackList) new ObjectInputStream(new ByteArrayInputStream(response, bodyIndex, response.length - bodyIndex)).readObject();
			} catch (IOException | ClassNotFoundException ignored) {}
		}
		return null;
	}

	public PlaybackInfo INFO() {
		String serializedObjName = responseMsg.substring(
			responseMsg.indexOf("serialized-object ") + "serialized-object ".length(),
			responseMsg.indexOf('\n', responseMsg.indexOf("serialized-object "))
		);
		if (serializedObjName.equals(PlaybackInfo.class.getName())) {
			try {
				return (PlaybackInfo) new ObjectInputStream(new ByteArrayInputStream(response, bodyIndex, response.length - bodyIndex)).readObject();
			} catch (IOException | ClassNotFoundException ignored) {}
		}
		return null;
	}

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
