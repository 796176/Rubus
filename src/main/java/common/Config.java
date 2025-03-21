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

package common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Config is an auxiliary class to manage simple, flat config files. A config files consists of fields separated with
 * a line feeder. A field itself is a key and a value both of which are represented as strings and separated with
 * a space character. If a space character is the only and last character of the field it makes a value
 * an empty string "". If a space character is the first character of the field it makes a key an empty string "".<br>
 * No key must have any space characters; because the first space character is the key-value separator.
 */
public class Config {

	private final Path configPath;

	private final NavigableMap<String, String> params = new TreeMap<>();

	/**
	 * Construct an instance of this class.
	 * @param configPath a path of the config
	 * @throws IOException if some I/O error occurs
	 */
	public Config(Path configPath) throws IOException {
		assert configPath != null;

		this.configPath = configPath;

		String configData = Files.readString(configPath);
		int lastLFChar = -1;
		int nextLFChar = configData.indexOf('\n', lastLFChar);
		while (true){
			String currentLine =
				configData.substring(lastLFChar + 1, nextLFChar == -1 ? configData.length() : nextLFChar);
			if (!currentLine.isBlank()) {
				int deliminatorIndex = currentLine.indexOf(' ');
				String key = currentLine.substring(0, deliminatorIndex);
				String value = currentLine.substring(deliminatorIndex + 1);
				params.put(key, value);
			}
			if (nextLFChar == -1) break;
			lastLFChar = nextLFChar;
			nextLFChar = configData.indexOf('\n', lastLFChar + 1);
		}
	}

	/**
	 * Adds or replaces a field.
	 * @param key the key
	 * @param value the value
	 */
	public void set(String key, String value) {
		assert key != null && value != null;

		params.put(key, value);
	}

	/**
	 * Returns the value by the key or null if the field is absent.
	 * @param key the key
	 * @return the value
	 */
	public String get(String key) {
		return params.get(key);
	}

	/**
	 * Removes the field.
	 * @param key the key
	 */
	public void remove(String key) {
		params.remove(key);
	}

	/**
	 * Saves all the fields to the file specified in the constructor. The behaviour is the same as calling
	 * duplicate(configPath).
	 * @throws IOException if some I/O error occurs
	 */
	public void save() throws IOException {
		duplicate(configPath);
	}

	/**
	 * Saves all the fields to the specified file. The order in which fields are saved is based on the order of the keys.
	 * @param dest the location of the file
	 * @throws IOException if some I/O error occurs
	 */
	public void duplicate(Path dest) throws IOException {
		try (ByteChannel wbc = Files.newByteChannel(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			Map.Entry<String, String> entry = params.firstEntry();
			while (entry != null) {
				wbc.write(ByteBuffer.wrap((entry.getKey() + " " + entry.getValue() + "\n").getBytes()));
				entry = params.higherEntry(entry.getKey());
			}
		}
	}
}
