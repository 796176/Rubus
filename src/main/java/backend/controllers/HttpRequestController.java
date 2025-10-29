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

package backend.controllers;

import backend.exceptions.InvalidParameterException;
import backend.models.WebRequestOriginator;
import backend.models.MediaFetch;
import backend.models.MediaInfo;
import backend.models.MediaList;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * HttpRequestController accepts HTTP requests and maps them to respective {@link RequestProcessor} methods based on
 * query parameters.<br>
 * Not intended to be used directly.
 */
@RestController
@RequestMapping("/")
public class HttpRequestController {

	private final Logger logger = LoggerFactory.getLogger(HttpRequestController.class);

	@Autowired
	private RequestProcessor requestProcessor;

	@GetMapping(params = "request_type=LIST")
	public Callable<MediaList> listRequest(
		@RequestParam("search_query") String searchQuery, HttpServletResponse response, HttpServletRequest request
	) {
		if (logger.isInfoEnabled()) {
			logger.info(
				"{} to process GET {}",
				this,
				constructFullURL(request.getRequestURL().toString(), request.getQueryString())
			);
		}
		return () -> {
			response.setContentType("application/octet-stream");
			return requestProcessor.listRequest(searchQuery, new WebRequestOriginator(request.getSession().getId()));
		};
	}

	@GetMapping(params = "request_type=INFO")
	public Callable<MediaInfo> infoRequest(
		@RequestParam("media_id") String mediaId, HttpServletResponse response, HttpServletRequest request
	) {
		if (logger.isInfoEnabled()) {
			logger.info(
				"{} to process GET {}",
				this,
				constructFullURL(request.getRequestURL().toString(), request.getQueryString())
			);
		}
		return () -> {
			response.setContentType("application/octet-stream");
			UUID id;
			try {
				id = UUID.fromString(mediaId);
			} catch (IllegalArgumentException e) {
				throw new InvalidParameterException();
			}
			return requestProcessor.infoRequest(id, new WebRequestOriginator(request.getSession().getId()));
		};
	}

	@GetMapping(params = "request_type=FETCH")
	public Callable<MediaFetch> fetchRequest(
		@RequestParam("media_id") String mediaId,
		@RequestParam("clip_offset") int clipOffset,
		@RequestParam("clip_amount") int clipAmount,
		HttpServletResponse response,
		HttpServletRequest request
	) {
		if (logger.isInfoEnabled()) {
			logger.info(
				"{} to process GET {}",
				this,
				constructFullURL(request.getRequestURL().toString(), request.getQueryString())
			);
		}
		return () -> {
			response.setContentType("application/octet-stream");
			UUID id;
			try {
				id = UUID.fromString(mediaId);
			} catch (IllegalArgumentException e) {
				throw new InvalidParameterException();
			}
			return requestProcessor
				.fetchRequest(id, clipOffset, clipAmount, new WebRequestOriginator(request.getSession().getId()));
		};
	}

	private String constructFullURL(String url, String urlParameters) {
		StringBuilder uri = new StringBuilder(url);
		if (!urlParameters.isEmpty()) uri.append("?").append(urlParameters);
		return uri.toString();
	}
}
