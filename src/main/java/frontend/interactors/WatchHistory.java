/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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

package frontend.interactors;

import java.io.Closeable;
import java.io.IOException;

/**
 * WatchHistory defines a data structure that stores watch history. At its core, the watch history of is a set of
 * key-value pairs, where the key is a media id and the value is the media progress in seconds.
 */
public interface WatchHistory extends Closeable {

	/**
	 * Sets the media progress associated with the given media id.
	 * @param mediaId the media id
	 * @param progress the media progress
	 * @throws IOException if some I/O exception occurs
	 */
	void setProgress(String mediaId, int progress) throws IOException;

	/**
	 * Returns the media progress associated the given media id. If the file doesn't contain the media id, the method
	 * returns -1.
	 * @param mediaId the media id
	 * @return the media progress
	 */
	int getProgress(String mediaId);

	/**
	 * Clears the watch history.
	 * @throws IOException if some I/O exception occurs
	 */
	void purge() throws IOException;
}
