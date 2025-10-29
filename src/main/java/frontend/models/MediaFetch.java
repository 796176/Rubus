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

package frontend.models;

/**
 * MediaFetch stores media content of the specified range. The range size is equal to video or audio array size.
 * @param id the media id
 * @param offset the index of the first clip
 * @param video an array of video clips
 * @param audio an array of audio clips
 */
public record MediaFetch(
	String id,
	int offset,
	byte[][] video,
	byte[][] audio
) { }
