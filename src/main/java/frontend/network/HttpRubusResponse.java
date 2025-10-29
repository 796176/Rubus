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

package frontend.network;

import frontend.adapters.ArraySeekableByteChannel;
import frontend.converters.BinaryConverter;
import frontend.converters.MediaFetchBinaryConverter;
import frontend.converters.MediaInfoBinaryConverter;
import frontend.converters.MediaListBinaryConverter;
import frontend.models.MediaFetch;
import frontend.models.MediaInfo;
import frontend.models.MediaList;
import jakarta.annotation.Nonnull;

import java.io.IOException;

/**
 * A concrete implementation of {@link RubusResponse} that converts HTTP status codes into rubus response types and HTTP
 * bodies into requested models.
 */
public class HttpRubusResponse implements RubusResponse {

	private final byte[] responseBody;

	private final int httpStatusCode;

	private BinaryConverter<MediaList> mediaListBinaryConverter = new MediaListBinaryConverter();

	private BinaryConverter<MediaInfo> mediaInfoBinaryConverter = new MediaInfoBinaryConverter();

	private BinaryConverter<MediaFetch> mediaFetchBinaryConverter = new MediaFetchBinaryConverter();

	/**
	 * Constructs an instance of this class.
	 * @param responseBody the content of the response body
	 * @param httpStatusCode the http response status code
	 */
	public HttpRubusResponse(@Nonnull byte[] responseBody, int httpStatusCode) {
		this.responseBody = responseBody;
		this.httpStatusCode = httpStatusCode;
	}

	@Override
	public RubusResponseType getResponseType() {
		return switch (httpStatusCode) {
			case 200 -> RubusResponseType.OK;
			case 400 -> RubusResponseType.BAD_REQUEST;
			default -> RubusResponseType.SERVER_ERROR;
		};
	}

	@Override
	public MediaList LIST() {
		try {
			return mediaListBinaryConverter.convert(new ArraySeekableByteChannel(responseBody));
		} catch (IOException ignored) { throw new RuntimeException(); }
	}

	@Override
	public MediaInfo INFO() {
		try {
			return mediaInfoBinaryConverter.convert(new ArraySeekableByteChannel(responseBody));
		} catch (IOException ignored) { throw new RuntimeException(); }
	}

	@Override
	public MediaFetch FETCH() {
		try {
			return mediaFetchBinaryConverter.convert(new ArraySeekableByteChannel(responseBody));
		} catch (IOException ignored) { throw new RuntimeException(); }
	}

	/**
	 * Returns the current {@link MediaList} converter.
	 * @return the current {@link MediaList} converter
	 */
	@Nonnull
	public BinaryConverter<MediaList> getMediaListBinaryConverter() {
		return  mediaListBinaryConverter;
	}

	/**
	 * Sets a new {@link MediaList} converter.
	 * @param newMediaListBinaryConverter a new {@link MediaList} converter
	 */
	public void setMediaListBinaryConverter(@Nonnull BinaryConverter<MediaList> newMediaListBinaryConverter) {
		mediaListBinaryConverter = newMediaListBinaryConverter;
	}

	/**
	 * Returns the current {@link MediaInfo} converter.
	 * @return the current {@link MediaInfo} converter
	 */
	@Nonnull
	public BinaryConverter<MediaInfo> getMediaInfoBinaryConverter() {
		return mediaInfoBinaryConverter;
	}

	/**
	 * Sets a new {@link MediaInfo} converter.
	 * @param newMediaInfoBinaryConverter a new {@link MediaInfo} converter
	 */
	public void setMediaInfoBinaryConverter(@Nonnull BinaryConverter<MediaInfo> newMediaInfoBinaryConverter) {
		mediaInfoBinaryConverter = newMediaInfoBinaryConverter;
	}

	/**
	 * Returns the current {@link MediaFetch} converter.
	 * @return the current {@link MediaFetch} converter
	 */
	@Nonnull
	public BinaryConverter<MediaFetch> getMediaFetchBinaryConverter() {
		return mediaFetchBinaryConverter;
	}

	/**
	 * Sets a new {@link MediaFetch} converter.
	 * @param newMediaFetchBinaryConverter a new {@link MediaFetch} converter
	 */
	public void setMediaFetchBinaryConverter(@Nonnull BinaryConverter<MediaFetch> newMediaFetchBinaryConverter) {
		mediaFetchBinaryConverter = newMediaFetchBinaryConverter;
	}
}
