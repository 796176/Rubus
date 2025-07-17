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

package frontend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WatchHistory defines a data structure that stores watch history. At its core, the watch history of is a set of
 * key-value pairs, where the key is a media id and the value is the media progress in seconds. WatchHistory is
 * associated with a file.<br>
 * Thread safety is achieved by acquiring a lock on this object before calling any method.
 */
public class WatchHistory implements Closeable {

	private final Logger logger = LoggerFactory.getLogger(WatchHistory.class);

	private final Path watchHistoryPath;

	private String watchHistory;

	private RandomAccessFile raf;

	/**
	 * Constructs an instance of this class using the given location of the watch history file. If the file doesn't
	 * exist the constructor attempts to create it.
	 * @param watchHistoryPath the location of the watch history file
	 * @throws IOException if some I/O exception occurs
	 */
	public WatchHistory(Path watchHistoryPath) throws IOException {
		assert watchHistoryPath != null;

		this.watchHistoryPath = watchHistoryPath;
		raf = new RandomAccessFile(watchHistoryPath.toFile(), "rw");
		watchHistory = Files.readString(watchHistoryPath);

		logger.debug("{} instantiated, Path: {}", this, watchHistoryPath);
	}

	/**
	 * Sets the media progress associated with the given media id; the file modification is limited to the part where
	 * the progress value resides, no extra bytes are added/removed or unnecessary rewritten. If the file doesn't
	 * contain the media id, a new pair is created and immediately appended to the file. If the current media id
	 * progress is equal to the new one, the method does nothing.
	 * @param mediaId the media id
	 * @param progress the media progress
	 * @throws IOException if some I/O exception occurs
	 */
	public synchronized void setProgress(String mediaId, int progress) throws IOException {
		assert mediaId != null && progress >= 0;

		// the progress value occupies 8 character
		String newRecord = "%s %08x\n".formatted(mediaId, progress);
		Pattern pattern = Pattern.compile(mediaId + " [0-9a-f]{8}\n");
		Matcher matcher = pattern.matcher(watchHistory);
		if (!matcher.find()) {
			raf.seek(watchHistory.getBytes().length);
			raf.write(newRecord.getBytes());
			watchHistory = watchHistory + newRecord;
		} else {
			String oldRecord = watchHistory.substring(matcher.start(), matcher.end());
			int oldProgress = Integer
				.parseUnsignedInt(oldRecord.substring(oldRecord.indexOf(' ') + 1, oldRecord.length() - 1), 16);
			if (oldProgress == progress) return;
			raf.seek(matcher.start() + mediaId.length() + 1);
			raf.write("%08x".formatted(progress).getBytes());
			watchHistory = watchHistory.replace(oldRecord, newRecord);
		}
	}

	/**
	 * Returns the media progress associated the given media id. If the file doesn't contain the media id, the method
	 * returns -1.
	 * @param mediaId the media id
	 * @return the media progress
	 */
	public synchronized int getProgress(String mediaId) {
		// the progress value occupies 8 character
		Pattern pattern = Pattern.compile(mediaId + " [0-9a-f]{8}\n");
		Matcher matcher = pattern.matcher(watchHistory);
		if (matcher.find()) {
			String record = watchHistory.substring(matcher.start(), matcher.end());
			return Integer.parseUnsignedInt(record.substring(record.indexOf(' ') + 1, record.length() - 1), 16);
		}
		return -1;
	}

	/**
	 * Clears the watch history.
	 * @throws IOException if some I/O exception occurs
	 */
	public synchronized void purge() throws IOException {
		close();
		Files.delete(watchHistoryPath);
		raf = new RandomAccessFile(watchHistoryPath.toFile(), "rw");
		watchHistory = "";

		logger.info("{} purged {}", this, watchHistoryPath);
	}

	/**
	 * Closes the underlying resources.
	 * @throws IOException if some I/O exception occurs
	 */
	@Override
	public synchronized void close() throws IOException {
		raf.close();

		logger.debug("{} closed", this);
	}
}
