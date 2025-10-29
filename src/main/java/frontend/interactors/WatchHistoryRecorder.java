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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * WatchHistoryRecorder links a particular instance of {@link WatchHistory} to an instance of {@link PlayerInterface}.
 * It tracks the progress of the currently playing media of the {@link PlayerInterface} instance and changes
 * the {@link WatchHistory} instance accordingly.
 */
public class WatchHistoryRecorder implements Observer {

	private final Logger logger = LoggerFactory.getLogger(WatchHistoryRecorder.class);

	private String id;

	private WatchHistory wh;

	private ExceptionHandler handler;

	/**
	 * Constructs an instance of this class.
	 * @param watchHistory the WatchHistory instance
	 * @param mediaId the media id the {@link PlayerInterface} instance is playing
	 * @param exceptionHandler the exception handler
	 */
	public WatchHistoryRecorder(WatchHistory watchHistory, String mediaId, ExceptionHandler exceptionHandler) {
		setWatchHistory(watchHistory);
		setMediaId(mediaId);
		setExceptionHandler(exceptionHandler);

		logger.debug(
			"{} instantiated, WatchHistory: {}, media id: {}, ExceptionHandler: {}",
			this,
			watchHistory,
			mediaId,
			exceptionHandler
		);
	}

	/**
	 * Constructs an instance of this class without an exception handler.
	 * @param watchHistory the WatchHistory instance
	 * @param mediaId the media id the {@link PlayerInterface} instance is playing
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
			logger.info("{} failed to update progress", wh, e);
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
	 * Exception classes that get passed to the handler:<br>
	 * &emsp;{@link IOException} if an I/O exception occurs in the underlying WatchHistory instance
	 * @param newExceptionHandler a new exception handler
	 */
	public void setExceptionHandler(ExceptionHandler newExceptionHandler) {
		assert newExceptionHandler != null;

		handler = newExceptionHandler;
	}

	/**
	 * Returns the current exception handler.<br<br>
	 *
	 * Exception classes that get passed to the handler:<br>
	 * &emsp;{@link IOException} if an I/O exception occurs in the underlying WatchHistory instance
	 * @return the current exception handler
	 */
	public ExceptionHandler getExceptionHandler() {
		return handler;
	}
}
