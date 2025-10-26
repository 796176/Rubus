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

package backend.models;

import backend.stubs.QueryingStrategyInterfaceStub;
import backend.stubs.SeekableByteChannelStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DefaultMediaTests {

	QueryingStrategyInterfaceStub queryingStrategyInterfaceStub = new QueryingStrategyInterfaceStub();

	DefaultMedia defaultMedia = new DefaultMedia(
		UUID.randomUUID(), "testing media", 2, URI.create("testing_uri"), queryingStrategyInterfaceStub
	);

	static Stream<Arguments> argumentProvider() {
		return Stream.of(
			Arguments.of(0, 1),
			Arguments.of(0, 2),
			Arguments.of(1, 1)
		);
	}

	@ParameterizedTest
	@MethodSource("argumentProvider")
	void videoClipsRetrievalTest(int offset, int amount) {
		SeekableByteChannel[] videoClips = new SeekableByteChannel[amount];
		Arrays.setAll(videoClips, i -> new SeekableByteChannelStub(new byte[0]));

		queryingStrategyInterfaceStub.queryFunction = names -> {
			assertEquals(amount, names.length, "The size of the array of names doesn't match");
			for (int i = offset; i < amount + offset; i++) {
				String expectedName = "v" + i;
				assertEquals(expectedName, names[i - offset], "The content of the array of names doesn't match");
			}
			return videoClips;
		};

		SeekableByteChannel[] retrievedClips = defaultMedia.retrieveVideoClips(offset, amount);
		assertSame(videoClips, retrievedClips, "The array of clips is a different array");
	}

	@ParameterizedTest
	@MethodSource("argumentProvider")
	void audioClipsRetrievalTest(int offset, int amount) {
		SeekableByteChannel[] audioClips = new SeekableByteChannel[amount];
		Arrays.setAll(audioClips, i -> new SeekableByteChannelStub(new byte[0]));

		queryingStrategyInterfaceStub.queryFunction = names -> {
			assertEquals(amount, names.length, "The size of the array of names doesn't match");
			for (int i = offset; i < amount + offset; i++) {
				String expectedName = "a" + i;
				assertEquals(expectedName, names[i - offset], "The content of the array of names doesn't match");
			}
			return audioClips;
		};

		SeekableByteChannel[] retrievedClips = defaultMedia.retrieveAudioClips(offset, amount);
		assertSame(audioClips, retrievedClips, "The array of clips is a different array");
	}
}
