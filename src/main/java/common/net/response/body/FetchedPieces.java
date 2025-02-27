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

package common.net.response.body;

import java.io.Serializable;

/**
 * FetchedPieces is a body part of the response message to the FETCH type of the request. It contains an array of video
 * pieces represented as byte arrays and an array of audio pieces represented as byte arrays.
 * @param id the media id of the required media
 * @param startingPieceIndex the starting index of the media required by the client
 * @param video the array containing video pieces
 * @param audio the array containing audio pieces
 */
public record FetchedPieces(
	String id,
	long startingPieceIndex,
	byte[][] video,
	byte[][] audio
) implements Serializable { }
