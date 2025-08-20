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

package backend.querying;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractQueryingStrategy provides basic functionality for {@link QueryingStrategyInterface}. This functionality
 * includes storage and retrieval of the environment variables and the translation from simple name of resources to
 * fully-qualified names. This class defines several template methods for name resolution and actual querying.
 */
public abstract class AbstractQueryingStrategy implements QueryingStrategyInterface {

	private final Logger logger = LoggerFactory.getLogger(AbstractQueryingStrategy.class);

	private final Map<String, Object> env = new ConcurrentHashMap<>();

	/**
	 * Sole constructor.
	 */
	public AbstractQueryingStrategy() {
		logger.debug("{} instantiated", this);
	}

	@Override
	public void close() { }

	@Override
	public Object addToEnvironment(String name, Object value) {
		return env.put(name, value);
	}

	@Override
	public Object removeFromEnvironment(String key) {
		return env.remove(key);
	}

	@Override
	public Map<String, Object> getEnvironment() {
		return env;
	}

	@Override
	public SeekableByteChannel query(String name) throws QueryingException {
		assert name != null;

		return fullyQualifiedQuery(compose(getRoot(), name));
	}

	@Override
	public SeekableByteChannel[] query(String[] names) throws QueryingException {
		assert names != null;

		return fullyQualifiedQuery(
			Arrays
				.stream(names)
				.map(n -> compose(getRoot(), Objects.requireNonNull(n)))
				.toArray(String[]::new)
		);
	}

	/**
	 * Returns the common parent for every queried resources
	 * @implSpec if the resource are files that reside in /home this method returns "/home"
	 * @return the root of the names of the resources
	 */
	protected abstract String getRoot();

	/**
	 * Composes a fully-qualified name from the provided octets.
	 * @implSpec for octets {"/home", "adam"} the return value is "/home/adam", and for octets {"com", "example}
	 * the return value is "example.com".
	 * @param octets a number of octets ordered from the most significant octet to the least significant octet
	 * @return a fully-qualified name
	 */
	protected abstract String compose(String... octets);

	/**
	 * Same as {@link #query(String)} but passes a fully-qualified name as an argument derived from invoking
	 * compose(getRoot(), name).
	 * @param fullyQualifiedName the fully-qualified name of the resource
	 * @return an {@link SeekableByteChannel} instance open for reading and containing the content of the resource
	 * @throws QueryingException if querying fails
	 */
	protected abstract SeekableByteChannel fullyQualifiedQuery(String fullyQualifiedName) throws QueryingException;

	/**
	 * Same as {@link #query(String)} but passes an array of fully qualified names as an argument derived from invoking
	 * compose(getRoot(), name) for each simple name.
	 * @param fullyQualifiedNames an array of fully qualified names of the resources
	 * @return an array of {@link SeekableByteChannel} instances open for reading and containing the contents of
	 *         the resources; their order corresponds the order of their fully qualified names in fullyQualifiedNames
	 * @throws QueryingException if querying fails
	 */
	protected abstract SeekableByteChannel[] fullyQualifiedQuery(String[] fullyQualifiedNames) throws QueryingException;
}
