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

package backend;

import backend.io.MediaPool;
import common.RubusSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SocketManager keeps track of open connections. It's responsible for handing the incoming requests, closing
 * the connections or keeping them open.
 */
public class SocketManager implements AutoCloseable {

	private final static Logger logger = LoggerFactory.getLogger(SocketManager.class);

	private final ExecutorService executorService;

	private boolean isTerminated = false;

	private final AtomicInteger activeConnections = new AtomicInteger(0);

	private final MediaPool mediaPool;

	private final RequestParserStrategy requestParserStrategy;

	private final ConcurrentLinkedQueue<RequestHandler> availableHandlers = new ConcurrentLinkedQueue<>();

	/**
	 * Constructs a new instance of this class.
	 * @param mediaPool the media pool containing the available media
	 * @param requestExecutorService the executor service that performs request handling
	 * @param requestParserStrategy the parser strategy to use
	 */
	public SocketManager(
		MediaPool mediaPool,
		ExecutorService requestExecutorService,
		RequestParserStrategy requestParserStrategy
	) {
		assert mediaPool != null && requestExecutorService != null && requestParserStrategy != null;

		this.mediaPool = mediaPool;
		executorService = requestExecutorService;
		this.requestParserStrategy = requestParserStrategy;
		logger.debug(
			"{} initialized, MediaPool: {}, ExecutorService: {}, RequestParserStrategy: {}",
			this,
			mediaPool,
			executorService,
			requestParserStrategy
		);
	}

	/**
	 * Adds a socket to handle its requests.
	 * @param socket the socket requests of which need to be handled
	 */
	public void add(RubusSocket socket) {
		assert socket != null;

		activeConnections.getAndIncrement();
		RequestHandler requestHandler;
		if (!availableHandlers.isEmpty()) {
			requestHandler = availableHandlers.poll();
			requestHandler.setRubusSocket(socket);
		} else {
			requestHandler =
				new RequestHandler(mediaPool, socket, requestParserStrategy.clone(), this::requestHandlerCallback);
		}
		executorService.submit(requestHandler);
	}

	/**
	 * Closes this SocketManager and all the open connections.
	 */
	@Override
	public void close() {
		isTerminated = true;
		for (Runnable runnable: executorService.shutdownNow()) {
			try {
				((RequestHandler) runnable).getRubusSocket().close();
			} catch (IOException e) {
				logger.warn("{} could not close connection", this, e);
			}
		}
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.info("ExecutorService {} shutdown interrupted in {}", executorService, this, e);
		}
		logger.debug("{} closed", this);
	}

	/**
	 * Returns the number of the currently open connections.
	 * @return the number of the currently open connections
	 */
	public int getOpenConnections() {
		return activeConnections.get();
	}

	private void requestHandlerCallback(RequestHandler requestHandler) {
		assert requestHandler != null;

		RequestHandler.Status status = requestHandler.getRequestHandlerStatus();
		boolean isTimeoutException =
			status.getExecutionStatus() == RequestHandler.ExecutionStatus.EXCEPTION &&
			status.getException() instanceof SocketTimeoutException;
		if (
			!isTerminated &&
			(status.getExecutionStatus() == RequestHandler.ExecutionStatus.SUCCESS || isTimeoutException)
		) {
			executorService.submit(requestHandler);
		} else {
			try {
				requestHandler.getRubusSocket().close();
				logger.info(
					"{} closed connection {}, total open connections: {}",
					this,
					requestHandler.getRubusSocket(),
					getOpenConnections() - 1
				);
			} catch (IOException e) {
				logger.warn("{} could not close connection", this, e);
			}
			activeConnections.getAndDecrement();
			availableHandlers.add(requestHandler);
		}
	}
}
