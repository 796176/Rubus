/*
 * Rubus is an application layer protocol for video and audio streaming and
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

package backend.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * TitledMediaProxy stores only the media pool, the id, and the title. Accessing other media-specific information
 * will be delegated to the subject.
 */
public class TitledMediaProxy extends MediaProxy {

	private final static Logger logger = LoggerFactory.getLogger(TitledMediaProxy.class);

	private final String title;

	/**
	 * Constructs an instance of this class
	 * @param mediaPool the mediaPool
	 * @param mediaID the media id
	 * @param title the title
	 */
	public TitledMediaProxy(MediaPool mediaPool, UUID mediaID, String title) {
		super(mediaPool, mediaID);
		assert title != null;

		this.title = title;
		logger.debug(
			"{} instantiated, MediaPool: {}, id: {}, title: {}", this, mediaPool, mediaID, title
		);
	}

	@Override
	public String getTitle() {
		return title;
	}
}
