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

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WatchHistory isolates clients from accessing the watch history file and the data composition of the file. At its
 * core, the watch history is a set of key-value pairs, where the key is a media id and the value is media progress in
 * seconds. WatchHistory is implemented to be able to handle a medium file size, approximately from several hundred to
 * several thousand pairs; the more specific approximation would require making assumptions about the media id size,
 * the underlying file system, etc.<br>
 * Thread safety is achieved by acquiring a lock on this object before calling any method.
 */
public class WatchHistory implements Closeable {

	private final Path watchHistoryPath;

	private String watchHistory;

	private RandomAccessFile raf;

	/**
	 * Constructs an instance of this class using the given file location of the watch history file. If the file doesn't
	 * exist the constructor attempts to create it.
	 * @param watchHistoryPath the location of the watch history file
	 * @throws IOException if some I/O exception occurs
	 */
	public WatchHistory(Path watchHistoryPath) throws IOException {
		assert watchHistoryPath != null;

		this.watchHistoryPath = watchHistoryPath;
		raf = new RandomAccessFile(watchHistoryPath.toFile(), "rw");
		watchHistory = Files.readString(watchHistoryPath);
	}

	/**
	 * Sets the media progress to the given media id; the file modification is limited to the part where the progress
	 * value resides, no extra bytes are added/removed or unnecessary rewritten. If the file doesn't contain
	 * the media id a new pair is created and immediately appended to the file. If the current media id progress is
	 * equal to the new one, the method does nothing.
	 * @param mediaId the media id
	 * @param progress the media progress
	 * @throws IOException if some I/O exception occurs
	 */
	public synchronized void setProgress(String mediaId, int progress) throws IOException {
		assert mediaId != null && progress >= 0;

		// 8 is the size of a hexadecimal string representation of 4 byte integer
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
	 * Returns the media progress of the given media id. If the file doesn't contain the media id, the method
	 * returns -1. This method doesn't access the file eliminating the I/O bottleneck.
	 * @param mediaId the media id
	 * @return the media progress
	 */
	public synchronized int getProgress(String mediaId) {
		// 8 is the size of a hexadecimal string representation of 4 byte integer
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
	}

	/**
	 * Closes the underlying resources.
	 * @throws IOException if some I/O exception occurs
	 */
	@Override
	public synchronized void close() throws IOException {
		raf.close();
	}
}
