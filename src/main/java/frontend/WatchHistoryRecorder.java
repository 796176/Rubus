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

package frontend;

import java.io.IOException;

/**
 * WatchHistoryRecorder serves as a bridge between a {@link PlayerInterface} instance and a {@link WatchHistory} class.
 * The media id provided to this object and the media progress provided by the {@link PlayerInterface} instance get
 * saved to the watch history file by delegating the process to the {@link WatchHistory} class.
 */
public class WatchHistoryRecorder implements Observer {

	private String id;

	private WatchHistory wh;

	private ExceptionHandler handler;

	/**
	 * Constructs an instance of this class.
	 * @param watchHistory the WatchHistory instance
	 * @param mediaId the media id
	 * @param exceptionHandler the exception handler
	 */
	public WatchHistoryRecorder(WatchHistory watchHistory, String mediaId, ExceptionHandler exceptionHandler) {
		setWatchHistory(watchHistory);
		setMediaId(mediaId);
		setExceptionHandler(exceptionHandler);
	}

	/**
	 * Constructs an instance of this class without an exception handler.
	 * @param watchHistory the WatchHistory instance
	 * @param mediaId the media id
	 */
	public WatchHistoryRecorder(WatchHistory watchHistory, String mediaId) {
		this(watchHistory, mediaId, e -> {});
	}

	@Override
	public void update(Subject s) {
		try {
			if (s instanceof PlayerInterface pi) {
				wh.setProgress(id, pi.getProgress());
			}
		} catch (IOException e) {
			handler.handleException(e);
		}
	}

	/**
	 * Sets a new media id.
	 * @param newMediaId a new media id
	 */
	public void setMediaId(String newMediaId) {
		assert newMediaId != null;

		id = newMediaId;
	}

	/**
	 * Returns the current media id.
	 * @return the current media id
	 */
	public String getMediaId() {
		return id;
	}

	/**
	 * Sets a new instance of WatchHistory.
	 * @param newWatchHistory a new instance of WatchHistory
	 */
	public void setWatchHistory(WatchHistory newWatchHistory) {
		assert newWatchHistory != null;

		wh = newWatchHistory;
	}

	/**
	 * Returns the current instance of WatchHistory.
	 * @return the current instance of WatchHistory
	 */
	public WatchHistory getWatchHistory() {
		return wh;
	}

	/**
	 * Sets a new exception handler.<br<br>
	 *
	 * The passed exception:
	 *     {@link IOException} if an I/O exception occurs in the underlying WatchHistory instance
	 * @param newExceptionHandler a new exception handler
	 */
	public void setExceptionHandler(ExceptionHandler newExceptionHandler) {
		assert newExceptionHandler != null;

		handler = newExceptionHandler;
	}

	/**
	 * Returns the current exception handler.<br<br>
	 *
	 * The passed exception:
	 *     {@link IOException} if an I/O exception occurs in the underlying WatchHistory instance
	 * @return the current exception handler
	 */
	public ExceptionHandler getExceptionHandler() {
		return handler;
	}
}
