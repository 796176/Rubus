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

package backend.io;

import common.net.response.body.PlaybackInfo;

import java.io.IOException;

public interface Media {
	String getID() throws IOException;
	String getTitle() throws IOException;
	int getDuration() throws IOException;
	int getVideoWidth() throws IOException;
	int getVideoHeight() throws IOException;
	String getVideoCodec() throws IOException;
	String getAudioCodec() throws IOException;
	String getVideoContainer() throws IOException;
	String getAudioContainer() throws IOException;
	byte[][] fetchAudioPieces(int pieceIndex, int number) throws IOException;
	byte[][] fetchVideoPieces(int pieceIndex, int number) throws IOException;
	PlaybackInfo toPlaybackInfo() throws IOException;
}
