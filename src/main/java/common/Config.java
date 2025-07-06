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

package common;

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
 * Config is an auxiliary class to manage simple, flat config files. A config files consists of fields separated with
 * a line feeder. A field itself is a key and a value both of which are represented as strings and separated with
 * a space character. If a space character is the only and last character of the field it makes a value
 * an empty string "". If a space character is the first character of the field it makes a key an empty string "".<br>
 * No key must have any space characters; because the first space character is the key-value separator.<br>
 * Thread safety is achieved by acquiring a lock on this object before calling any method.
 */
public class Config {

	/**
	 * ConfigFunction is similar to Java's {@link java.util.function.Function}, but the argument type is bound to Config
	 * and a checked exception can be thrown inside the body.
	 * @param <T> the type of the result of the function
	 * @param <E> the type of the exception the function may throw
	 */
	@FunctionalInterface
	public interface ConfigFunction<T, E extends Exception> {
		/**
		 * Applies this function to the given Config.
		 * @param c a Config instance
		 * @return the lambda function result
		 * @throws E if an exception is thrown inside the lambda function body
		 */
		T apply(Config c) throws E;
	}

	private final Logger logger = LoggerFactory.getLogger(Config.class);

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

		logger.debug("{} instantiated, Path: {}", this, configPath);
	}

	private Config() {
		configPath = null;

		logger.debug("{} instantiated with no config path", this);
	}

	/**
	 * Creates a new instance of this class, populates it with the kay-value pairs,
	 * and saves it to the specified location. If the specified files or the parent directory don't exist, the method
	 * creates them. If the specified file exists, its content is rewritten.
	 * @param configPath the location of the created config file
	 * @param values alternating keys and values, the amount must be even
	 * @return an instance of this class
	 * @throws IOException if some I/O error occurs
	 */
	public static Config create(Path configPath, String... values) throws IOException {
		assert configPath != null && values != null && values.length % 2 == 0;

		Files.createDirectories(configPath.getParent());
		Files.newByteChannel(
			configPath,
			StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
		).close();
		Config config = new Config(configPath);
		for (int i = 0; i < values.length; i += 2) {
			config.set(values[i], values[i + 1]);
		}
		config.save();
		return config;
	}

	/**
	 * Adds or replaces a field.
	 * @param key the key
	 * @param value the value
	 */
	public synchronized void set(String key, String value) {
		assert key != null && value != null;

		params.put(key, value);
	}

	/**
	 * Returns the value by the key or null if the field is absent.
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
	 * Allows the caller to perform as many calls on this Config as necessary, blocking other threads from accessing or
	 * modifying this Config. The method is designed to be flexible so a checked exception can be thrown inside
	 * the lambda function body, which will be propagated to the caller; the lambda function can return any object,
	 * which will be returned by this method.<br>
	 * The main purpose of this method as opposed to simply calling Config methods directly is an intermediate state this
	 * Config object may have while performing modifications and other threads shouldn't be able to access this state.
	 * Let's assume, for example, that a Config consists of two fields: an ip address and a port; and the values are
	 * already set and can be accessed by some thread to create sockets. Then, if a different thread wants to change
	 * both ip address and the port, it doesn't want another thread to access a new ip address value but access the old
	 * port value. And this can happen if the thread calls {@link #set(String, String)} two times even if these calls
	 * are consecutive. And in order to avoid this the thread must modify both the ip address and port values inside
	 * the lambda function body.
	 * @param a an action this method performs supplying it with this Config
	 * @return the object the lambda function returns
	 * @param <T> the object type the lambda function returns
	 * @param <E> the exception type that can be thrown inside the lambda body function
	 * @throws E if the lambda function throws an exception
	 */
	public synchronized <T, E extends Exception> T action(ConfigFunction<T, E> a) throws E {
		return a.apply(this);
	}

	/**
	 * Saves all the fields to the file specified in the constructor. The behaviour is the same as calling
	 * duplicate(configPath).
	 * @throws IOException if some I/O error occurs
	 */
	public synchronized void save() throws IOException {
		duplicate(configPath);
	}

	/**
	 * Saves all the fields to the specified file. The order in which fields are saved is based on the order of the keys.
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
	 * Returns an immutable instance of this Config. Because thread safety of Config is achieved through locking,
	 * it results in performance loss even if the operations themselves are thread safe ( e.g. only {@link #get(String)}
	 * invocations ). The Config instance returned by this method doesn't use locking on any of its methods, but it has
	 * the following limitations: <br>
	 * 1. Attempts to call any modification method like {@link #set(String, String)}, {@link #remove(String)}, etc. will
	 * throw {@link UnsupportedOperationException}<br>
	 * 2. Attempts to call {@link #save()} will be ignored<br>
	 * 3. Attempts to call {@link #duplicate(Path)} will be ignored if and only if the passed parameter is equal to
	 * configPath, otherwise it will throw {@link UnsupportedOperationException}<br>
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
