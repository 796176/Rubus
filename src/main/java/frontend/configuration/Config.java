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

package frontend.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Config is an auxiliary class to manage simple, flat config files. A config file consists of fields separated with
 * a line feed (0a). A field itself is a key-value pair represented as a string. A key is separated from a value with
 * a space character (20). If there is only one space character in the filed, and it is the last character of the field
 * it makes the value an empty string "". If a space character is the first character of the field it makes the key
 * an empty string "". If the field contains multiple space characters, the first character is the one that separates
 * the key from the value.<br>
 * Thread safety of Config instances is achieved by acquiring a lock on this object before calling any method.
 */
public class Config {

	/**
	 * ConfigFunction is similar to Java's {@link java.util.function.Function}, but the parameter is always the Config
	 * type. The exception {@link ConfigFunction#apply(Config)} throws is parameterized.
	 * @param <T> the type of the result of the function
	 * @param <E> the type of the exception the function may throw
	 */
	@FunctionalInterface
	public interface ConfigFunction<T, E extends Exception> {
		/**
		 * Applies this function to the given Config.
		 * @param c the Config instance
		 * @return the lambda function result
		 * @throws E if the exception is thrown inside the lambda function body
		 */
		T apply(Config c) throws E;
	}

	private final Logger logger = LoggerFactory.getLogger(Config.class);

	private final Path configPath;

	private final NavigableMap<String, String> params = new TreeMap<>();

	/**
	 * Constructs an instance of this class.
	 * @param configPath the location of the config file
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

		logger.debug("{} instantiated, Path: {}", this, configPath);
	}

	private Config() {
		configPath = null;

		logger.debug("{} instantiated with no config path", this);
	}

	/**
	 * Adds a new field, or sets a new value.
	 * @param key the key
	 * @param value the value
	 */
	public synchronized void set(String key, String value) {
		assert key != null && value != null;

		params.put(key, value);
	}

	/**
	 * Returns the value associated with the key, or null if the field is absent.
	 * @param key the key
	 * @return the value
	 */
	public synchronized String get(String key) {
		return params.get(key);
	}

	/**
	 * Removes the field.
	 * @param key the key
	 */
	public synchronized void remove(String key) {
		params.remove(key);
	}

	/**
	 * Allows to perform many calls on this Config in a single batch. The method is designed to be flexible so a checked
	 * exception can be thrown inside the lambda function body, which will be propagated to the caller; the lambda
	 * function can return any object, which will be returned by this method.<br>
	 * The main purpose of this method as opposed to simply calling Config methods directly is to block other threads
	 * from accessing this Config instance while the lambda function body is executed. Let's assume, for example, that
	 * Config has two fields: an ip address and a port; and the values are already set and can be accessed by some
	 * thread. Then, if a different thread wants to change both ip address and the port, and it doesn't want another
	 * thread to access a new ip address value but access the old port value. And this can happen if the thread calls
	 * {@link #set(String, String)} two times even if these calls are consecutive. So, in order to avoid this,
	 * the thread must modify both the ip address and port values inside the lambda function body.
	 * @param a the action this method performs passing this Config instance as an argument
	 * @return the object the lambda function returns
	 * @param <T> the object type the lambda function returns
	 * @param <E> the exception type that can be thrown inside the lambda body function
	 * @throws E if the lambda function throws an exception
	 */
	public synchronized <T, E extends Exception> T action(ConfigFunction<T, E> a) throws E {
		return a.apply(this);
	}

	/**
	 * Saves this Config to the file located at configPath. It is the same as calling duplicate(configPath).
	 * @throws IOException if some I/O error occurs
	 */
	public synchronized void save() throws IOException {
		duplicate(configPath);
	}

	/**
	 * Saves this Config to the file located at dest.
	 * @param dest the location of the file
	 * @throws IOException if some I/O error occurs
	 */
	public synchronized void duplicate(Path dest) throws IOException {
		try (
			ByteChannel wbc = Files.newByteChannel(
					dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
			)
		) {
			Map.Entry<String, String> entry = params.firstEntry();
			while (entry != null) {
				wbc.write(ByteBuffer.wrap((entry.getKey() + " " + entry.getValue() + "\n").getBytes()));
				entry = params.higherEntry(entry.getKey());
			}

			logger.debug("{}'s content is written to {}", this, dest);
		}
	}

	/**
	 * Returns an immutable instance of this Config. Thread safety of Config is achieved through locking, so it may
	 * result in performance loss even if the operations themselves are thread safe ( e.g. only
	 * {@link #get(String)} invocations ). The Config instance returned by this method doesn't use locking on any of its
	 * methods, but it has the following limitations: <br>
	 * 1. Attempts to call any modification method like {@link #set(String, String)}, {@link #remove(String)}, etc. will
	 * throw {@link UnsupportedOperationException}.<br>
	 * 2. Attempts to call {@link #save()} will be ignored.<br>
	 * 3. Attempts to call {@link #duplicate(Path)} will be ignored if and only if the passed argument is equal to
	 * configPath, otherwise it will throw {@link UnsupportedOperationException}.<br>
	 * @return an immutable Config
	 */
	public synchronized Config immutableConfig() {
		return new ImmutableConfig(configPath, params);
	}

	private static class ImmutableConfig extends Config {

		private final Logger logger = LoggerFactory.getLogger(ImmutableConfig.class);

		private final NavigableMap<String, String> params;

		private final Path configPath;

		private ImmutableConfig(Path configPath, NavigableMap<String, String> navigableMap) {
			assert configPath != null && navigableMap != null;

			this.configPath = configPath;
			params = navigableMap;

			if (logger.isDebugEnabled()) {
				String[] fields =
					navigableMap
						.entrySet()
						.stream()
						.map(entry-> entry.getKey() + ": " + entry.getValue())
						.toArray(String[]::new);
				logger.debug("{} instantiated, Path: {}, fields: {}", this, configPath, Arrays.toString(fields));
			}
		}

		@Override
		public void set(String key, String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String get(String key) {
			return params.get(key);
		}

		@Override
		public synchronized void remove(String key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T, E extends Exception> T action(ConfigFunction<T, E> a) throws E {
			return a.apply(this);
		}

		@Override
		public void save() { }

		@Override
		public void duplicate(Path dest) {
			if (!dest.equals(configPath)) throw new UnsupportedOperationException();
		}

		@Override
		public Config immutableConfig() {
			return this;
		}
	}
}
