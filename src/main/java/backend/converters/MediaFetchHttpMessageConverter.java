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

package backend.converters;

import backend.controllers.DataStreams;
import backend.models.MediaFetch;
import jakarta.annotation.Nonnull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.List;

/**
 * Converts {@link MediaFetch} into an HTTP response.<br>
 * Not intended to be used directly.
 */
@Component
public class MediaFetchHttpMessageConverter implements HttpMessageConverter<MediaFetch> {

	private final BinaryConverter<MediaFetch> mediaFetchBinaryConverter = new MediaFetchBinaryConverter();
	@Override
	public boolean canRead(@Nonnull Class<?> clazz, MediaType mediaType) {
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return clazz.equals(MediaFetch.class);
	}

	@Nonnull
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return List.of(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Nonnull
	@Override
	public MediaFetch read(
		@Nonnull Class<? extends MediaFetch> clazz, @Nonnull HttpInputMessage inputMessage
	) throws IOException, HttpMessageNotReadableException {
		throw new IOException();
	}

	@Override
	public void write(
		@Nonnull MediaFetch mediaFetch, MediaType contentType, HttpOutputMessage outputMessage
	) throws IOException, HttpMessageNotWritableException {
		try (SeekableByteChannel seekableByteChannel = mediaFetchBinaryConverter.convert(mediaFetch)) {
			DataStreams.passData(seekableByteChannel, outputMessage.getBody());
		} finally {
			for (SeekableByteChannel channel: mediaFetch.video()) {
				try { channel.close(); } catch (Exception ignored) { }
			}
			for (SeekableByteChannel channel: mediaFetch.audio()) {
				try { channel.close(); } catch (Exception ignored) { }
			}
		}
	}
}
