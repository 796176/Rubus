/*
 * Rubus is an application layer protocol for video and audio streaming and
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

import backend.io.Media;
import backend.io.MediaPool;
import common.RubusSocket;
import common.RubusSockets;
import common.net.response.RubusResponseType;
import common.net.response.body.FetchedPieces;
import common.net.response.body.MediaInfo;
import common.net.response.body.MediaList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.function.Consumer;

/**
 * RequestHandler is responsible for receiving clients' requests and sending the appropriate responses. RequestHandler
 * is designed in the way that the same instance can be used to process different requests, as well as an ability to
 * set different objects if it's necessary before invoking {@link #run()}. After the {@link #run()} execution
 * RequestHandler calls the callback function and passes itself as parameter. The client can query the details about
 * the execution by calling the {@link #getRequestHandlerStatus()} method.
 */
public class RequestHandler implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(RequestHandler.class);

	private RubusSocket socket;

	private MediaPool pool;

	private RequestParserStrategy parser;

	private Consumer<RequestHandler> callback;

	private RequestHandler.Status status;

	/**
	 * Creates a new instance of this class.
	 * @param mediaPool the media pool containing the available media
	 * @param socket the socket requests of which need to be handled
	 * @param requestParserStrategy the parser strategy to use
	 * @param callback the function this RequestHandler calls after being completed
	 */
	public RequestHandler(
		MediaPool mediaPool,
		RubusSocket socket,
		RequestParserStrategy requestParserStrategy,
		Consumer<RequestHandler> callback
	) {
		assert mediaPool != null && socket != null && requestParserStrategy != null && callback != null;

		this.socket = socket;
		this.pool = mediaPool;
		parser = requestParserStrategy;
		this.callback = callback;
		logger.debug(
			"{} instantiated, MediaPool: {} RubusSocket: {}, RequestParserStrategy: {}, callback: {}",
			this,
			mediaPool,
			socket,
			requestParserStrategy,
			callback
		);
	}

	/**
	 * Creates a new instance of this class without the callback function.
	 * @param mediaPool the media pool containing the available media
	 * @param socket the socket requests of which need to be handled
	 * @param requestParserStrategy the parser strategy to use
	 */
	public RequestHandler(MediaPool mediaPool, RubusSocket socket, RequestParserStrategy requestParserStrategy) {
		this(mediaPool, socket, requestParserStrategy, requestHandler -> {});
	}

	@Override
	public void run() {
		try {
			byte[] request;
			try {
				 request = RubusSockets.extractMessage(socket,300);
				 logger.info("{} retrieved request from {}", this, socket);
			} catch (Exception e) {
				logger.debug("{} could not retrieve request from {}", this, socket, e);
				status = new Status(ExecutionStatus.EXCEPTION, e);
				callback.accept(this);
				return;
			}

			String requestMes = new String(request);
			parser.feed(requestMes);
			StringBuilder responseMes = new StringBuilder("response-type ").append(RubusResponseType.OK).append('\n');
			ByteArrayOutputStream body = new ByteArrayOutputStream();
			RequestValueChecker rvc = new RequestValueChecker();
			switch (parser.type()) {
				case LIST -> {
					String titlePattern = parser.value("title-contains");
					ArrayList<String> ids = new ArrayList<>();
					ArrayList<String> titles = new ArrayList<>();
					for (Media m: pool.availableMediaFast()) {
						if (m.getTitle().matches(titlePattern)) {
							ids.add(HexFormat.of().formatHex(m.getID()));
							titles.add(m.getTitle());
						}
					}
					MediaList mediaList = new MediaList(ids.toArray(new String[0]), titles.toArray(new String[0]));
					responseMes.append("serialized-object ").append(MediaList.class.getName()).append('\n');
					ObjectOutputStream oos = new ObjectOutputStream(body);
					oos.writeObject(mediaList);
				}

				case INFO -> {
					String mediaID = rvc.checkId(parser.value("media-id"));
					Media media = pool.getMedia(HexFormat.of().parseHex(mediaID));
					MediaInfo mediaInfo = media.toMediaInfo();
					responseMes.append("serialized-object ").append(MediaInfo.class.getName()).append('\n');
					ObjectOutputStream oos = new ObjectOutputStream(body);
					oos.writeObject(mediaInfo);
				}

				case FETCH -> {
					String mediaID = rvc.checkId(parser.value("media-id"));
					int beginningPieceIndex =
						rvc.checkForNegative(Integer.parseInt(parser.value("starting-playback-piece")));
					int piecesToFetch =
						rvc.checkForNonPositive(Integer.parseInt(parser.value("total-playback-pieces")));
					Media media = pool.getMedia(HexFormat.of().parseHex(mediaID));
					FetchedPieces fetchedPieces =
						new FetchedPieces(
							mediaID,
							beginningPieceIndex,
							media.fetchVideoPieces(beginningPieceIndex, piecesToFetch),
							media.fetchAudioPieces(beginningPieceIndex, piecesToFetch)
						);
					responseMes.append("serialized-object ").append(FetchedPieces.class.getName()).append('\n');
					ObjectOutputStream oos = new ObjectOutputStream(body);
					oos.writeObject(fetchedPieces);
				}
			}
			responseMes.append("body-length ").append(body.size()).append("\n\n");
			byte[] response = Arrays.copyOf(responseMes.toString().getBytes(), responseMes.length() + body.size());
			System.arraycopy(body.toByteArray(), 0, response, responseMes.length(), body.size());
			try {
				socket.write(response);
				logger.info("{} sent response to {} with {} response type", this, socket, RubusResponseType.OK);
			} catch (IOException e) {
				logger.warn("{} could not send response to {}", this, socket, e);
			}
		} catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException e) {
			try {
				String errorMsg =
					"response-type " + RubusResponseType.BAD_REQUEST + "\n" +
					"body-length 0\n\n";
				socket.write(errorMsg.getBytes());
				logger.info(
					"{} sent response to {} with {} response type", this, socket, RubusResponseType.BAD_REQUEST
				);
			} catch (IOException ioException) {
				logger.warn("{} could not send response to {}", this, socket, e);
			}
		} catch (IOException | DataAccessException e) {
			logger.error("{} encountered internal error", this, e);
			try {
				String errorMsg =
					"response-type " + RubusResponseType.SERVER_ERROR + "\n" +
					"body-length 0\n\n";
				socket.write(errorMsg.getBytes());
				logger.info(
					"{} sent response to {} with {} response type", this, socket, RubusResponseType.SERVER_ERROR
				);
			} catch (IOException ioException) {
				logger.warn("{} could not send response to {}", this, socket, e);
			}
		}

		status = new Status(ExecutionStatus.SUCCESS, null);
		callback.accept(this);
	}

	/**
	 * Returns the status of this RequestHandler. The return value is updated every time on completion of {@link #run()}.
	 * @return the status of this RequestHandler
	 */
	public RequestHandler.Status getRequestHandlerStatus() {
		return status;
	}

	/**
	 * Returns the MediaPool reference.
	 * @return the MediaPool reference
	 */
	public MediaPool getMediaPool() {
		return pool;
	}

	/**
	 * Returns the RubusSocket reference.
	 * @return the RubusSocket reference
	 */
	public RubusSocket getRubusSocket() {
		return socket;
	}

	/**
	 * Returns the current callback function.
	 * @return the current callback function
	 */
	public Consumer<RequestHandler> getCallback() {
		return callback;
	}

	/**
	 * Returns the RequestParserStrategy reference.
	 * @return the RequestParserStrategy reference
	 */
	public RequestParserStrategy getRequestParserStrategy() {
		return parser;
	}

	/**
	 * Sets a new MediaPool reference.
	 * @param newMediaPool a new MediaPool
	 */
	public void setMediaPool(MediaPool newMediaPool) {
		assert newMediaPool != null;

		pool = newMediaPool;
	}

	/**
	 * Sets a new RubusSocket reference.
	 * @param newRubusSocket a new RubusSocket
	 */
	public void setRubusSocket(RubusSocket newRubusSocket) {
		assert newRubusSocket != null;

		socket = newRubusSocket;
	}

	/**
	 * Sets a new RequestParserStrategy reference.
	 * @param newRequestParserStrategy a new RequestParserStrategy
	 */
	public void setRequestParserStrategy(RequestParserStrategy newRequestParserStrategy) {
		assert newRequestParserStrategy != null;

		parser = newRequestParserStrategy;
	}

	/**
	 * Sets a new callback function.
	 * @param newCallback a new callback function
	 */
	public void setCallback(Consumer<RequestHandler> newCallback) {
		assert newCallback != null;

		callback = newCallback;
	}

	/**
	 * Defines the set of possible outcomes of {@link #run()} execution.
	 */
	public enum ExecutionStatus {
		/**
		 * The request was successfully processed.
		 */
		SUCCESS,

		/**
		 * The exception occurred while attempting to handle the request ( e.g. timeout exception, io exception etc. )
		 */
		EXCEPTION
	}

	/**
	 * RequestHandler.Status allows the client to get the detailed information about execution of this RequestHandler.
	 */
	public static class Status {

		private final static Logger logger = LoggerFactory.getLogger(Status.class);

		private final ExecutionStatus executionStatus;

		private final Exception e;

		private Status(ExecutionStatus executionStatus, Exception exception) {
			assert executionStatus != null;

			this.executionStatus = executionStatus;
			e = exception;
			logger.debug("{} instantiated, ExecutionStatus: {}, Exception: ", this, executionStatus, exception);
		}

		/**
		 * Returns the execution status.
		 * @return the execution status
		 */
		public ExecutionStatus getExecutionStatus() {
			return executionStatus;
		}

		/**
		 * If the execution status is EXCEPTION, returns the occurred exception.
		 * @return the occurred exception
		 */
		public Exception getException() {
			return e;
		}
	}
}