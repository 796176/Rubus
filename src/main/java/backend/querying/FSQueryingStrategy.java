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

import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * An implementation of {@link QueryingStrategyInterface} to query files that are located under a single directory.
 */
public class FSQueryingStrategy extends AbstractQueryingStrategy {

	private final Logger logger = LoggerFactory.getLogger(FSQueryingStrategy.class);

	private final Path directory;

	/**
	 * Constructs an instance of this class.
	 * @param path the location of the directory under which the queried files are located.
	 */
	public FSQueryingStrategy(Path path) {
		assert path != null;

		if (Files.notExists(path) || !Files.isDirectory(path)) {
			logger.error("{}: {} doesn't exist or not a directory", this, path);
			throw new IllegalArgumentException(path + " doesn't exist or it's not a directory");
		}
		directory = path;

		logger.debug("{} instantiated, Path: {}", this, path);
	}

	@Override
	protected String compose(String... octets) {
		StringBuilder sb = new StringBuilder(octets[0]);
		for (int i = 1; i < octets.length; i++) {
			if (!octets[i - 1].endsWith(File.separator)) sb.append(File.separator);
			sb.append(octets[i]);
		}
		return sb.toString();
	}

	@Override
	protected SeekableByteChannel fullyQualifiedQuery(String fullyQualifiedName) throws QueryingException {
		try {
			return Files.newByteChannel(Path.of(fullyQualifiedName), StandardOpenOption.READ);
		} catch (Exception e) {
			throw new QueryingException(e);
		}
	}

	@Override
	protected SeekableByteChannel[] fullyQualifiedQuery(String[] fullyQualifiedNames) throws QueryingException {
		SeekableByteChannel[] channels = new SeekableByteChannel[fullyQualifiedNames.length];
		try {
			for (int i = 0; i < fullyQualifiedNames.length; i++) {
				channels[i] = Files.newByteChannel(Path.of(fullyQualifiedNames[i]), StandardOpenOption.READ);
			}
		} catch (Exception e) {
			for (SeekableByteChannel channel: channels) {
				try { channel.close(); } catch (Exception ignored) { }
			}
			throw new QueryingException(e);
		}
		return channels;
	}

	@Override
	public String getRoot() {
		return directory.toString();
	}
}
