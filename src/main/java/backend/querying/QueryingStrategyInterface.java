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

import java.nio.channels.SeekableByteChannel;
import java.util.Map;

/**
 * The location of resources is often specified by the URI. The URI can point to local files ( if they reside on
 * the disk ) or remote files ( if they reside on another device which can be accessed via the Internet, etc. ).
 * The sheer variety of ways a resource can be accessed warrants a unified interface.<br><br>
 *
 * QueryingStrategyInterface assumes that resources have both simple names and fully-qualified names ( drawing an
 * analogy with Java's class names ). The client queries resources using simple names while the concrete implementations
 * translate them to fully qualified names based to their configurations.<br>
 * {@link FSQueryingStrategy} for example takes a location of a directory under which the resources are located. So,
 * the translation to a fully qualified name would be a concatenation of the directory name and the resource
 * name.<br><br>
 *
 * Querying of some resources requires more than just their URIs. If, for example, resources are located on a remote Web
 * server that requires authentication via a login and a password. These environment variables can be provided to
 * a concrete implementation via {@link #addToEnvironment(String, Object)}. The naming schema of environment variables
 * is up to the concrete implementations.
 */
public interface QueryingStrategyInterface extends AutoCloseable {

	/**
	 * Adds a new environment variable to this instance. If the variable with the same name already exists, its value
	 * is overwritten.
	 * @param name the variable name
	 * @param value the variable value
	 * @return the previous value or null
	 */
	Object addToEnvironment(String name, Object value);

	/**
	 * Removes the environment variable from this instance.
	 * @param key the variable name
	 * @return the removed variable value or null if the variable didn't exist
	 */
	Object removeFromEnvironment(String key);

	/**
	 * Returns the map containing environment variables of this instance. The effect of making changes to the returned
	 * map is undefined.
	 * @return the map of environment variables
	 */
	Map<String, Object> getEnvironment();

	/**
	 * Queries a resource of the given simple name. The name resolution is defined by the concrete implementations.
	 * @param name the simple name of the resource
	 * @return an {@link SeekableByteChannel} instance open for reading and containing the content of the resource
	 * @throws QueryingException if the querying fails
	 */
	SeekableByteChannel query(String name) throws QueryingException;

	/**
	 * Queries multiple resources of the given simple names. The name resolution is defined by the concrete
	 * implementations.
	 * @param names an array of simple names of the resources
	 * @return an array of {@link SeekableByteChannel} instances open for reading and containing the contents of
	 *         the resources; their order corresponds the order of their simple names in names
	 * @throws QueryingException if the querying fails
	 */
	SeekableByteChannel[] query(String[] names) throws QueryingException;
}
