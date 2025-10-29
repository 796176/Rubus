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

import backend.exceptions.AuthorizationException;
import backend.exceptions.InvalidHttpRequestException;
import backend.exceptions.CommonSecurityException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * ExceptionHandlingController is responsible for logging exceptions that occur in other controllers and mapping their
 * types to respective HTTP status codes.<br>
 * Not intended to be used directly.
 */
@ControllerAdvice
public class ExceptionHandlingController {

	private final Logger logger = LoggerFactory.getLogger(ExceptionHandlingController.class);

	@ExceptionHandler(AuthorizationException.class)
	void authorizationExceptionHandling(
		AuthorizationException e, HttpServletResponse response, HttpServletRequest request
	) {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		logger.info(
			"Exception encountered while processing {} {}, remote address: {}, http session id: {}",
			request.getMethod(),
			constructFullURL(request.getRequestURL().toString(), request.getQueryString()),
			request.getRemoteAddr(),
			request.getSession().getId(),
			e
		);
	}

	@ExceptionHandler(CommonSecurityException.class)
	void securityExceptionHandling(
		CommonSecurityException e, HttpServletResponse response, HttpServletRequest request
	) {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		logger.warn(
			"Exception encountered while processing {} {}, remote address: {}, http session id: {}",
			request.getMethod(),
			constructFullURL(request.getRequestURL().toString(), request.getQueryString()),
			request.getRemoteAddr(),
			request.getSession().getId(),
			e
		);
	}

	@ExceptionHandler(InvalidHttpRequestException.class)
	void invalidHttpRequestExceptionHandling(
		InvalidHttpRequestException e, HttpServletResponse response, HttpServletRequest request
	) {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		logger.info(
			"Exception encountered while processing {} {}, remote address: {}, http session id: {}",
			request.getMethod(),
			constructFullURL(request.getRequestURL().toString(), request.getQueryString()),
			request.getRemoteAddr(),
			request.getSession().getId(),
			e
		);
	}

	@ExceptionHandler(Exception.class)
	void exceptionHandling(Exception e, HttpServletResponse response, HttpServletRequest request) {
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		logger.warn(
			"Exception encountered while processing {} {}, remote address: {}, http session id: {}",
			request.getMethod(),
			constructFullURL(request.getRequestURL().toString(), request.getQueryString()),
			request.getRemoteAddr(),
			request.getSession().getId(),
			e
		);
	}

	private String constructFullURL(String url, String urlParameters) {
		StringBuilder uri = new StringBuilder(url);
		if (!urlParameters.isEmpty()) uri.append("?").append(urlParameters);
		return uri.toString();
	}
}
