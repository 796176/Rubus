/*
 * Rubus is an application level protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024-2025 Yegore Vlussove
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

package common.net.response.body;

import java.io.Serializable;

/**
 * MediaInfo is a body part of the response message to the INFO type of the request. It contains the meta-information
 * of the specified media.
 * @param id the id of the media specified by the client
 * @param title the title of the media
 * @param videoWidth the width of the video in pixels
 * @param videoHeight the height of the video in pixels
 * @param duration the duration of the media in seconds
 * @param videoEncoding the codec of the video
 * @param audioEncoding the codec of the audio
 * @param videoContainer the container of the video
 * @param audioContainer the container of the audio
 */
public record MediaInfo(
	String id,
	String title,
	int videoWidth,
	int videoHeight,
	int duration,
	String videoEncoding,
	String audioEncoding,
	String videoContainer,
	String audioContainer
) implements Serializable { }
