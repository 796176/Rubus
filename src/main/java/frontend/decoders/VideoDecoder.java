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

package frontend.decoders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * VideoDecoder is an abstract class that declares {@link DecodedFrames} and parameterizes {@link Decoder} with it.
 */
public abstract class VideoDecoder implements Decoder<VideoDecoder.DecodedFrames> {

	private final Logger logger = LoggerFactory.getLogger(VideoDecoder.class);

	public VideoDecoder() {
		logger.debug("{} instantiated", this);
	}

	/**
	 * DecodedFrames represents decoded frames as an array of Image objects.
	 * @param frames decoded frames
	 * @param offset the number of decoded frames skipped relative to the video clip; this number can be lesser than
	 *               the specified one but never greater
	 */
	public record DecodedFrames(Image[] frames, int offset) {}
}
