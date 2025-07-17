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
 * RequestHandler is responsible for receiving request messages, processing them, and sending response messages.
 * RequestHandler is designed so that the same instance can be reused multiple times to receive and process different
 * request messages.<br>
 * After the {@link #run()} method execution, RequestHandler invokes the callback function and passes itself as an
 * argument. The client can query the details about the execution of {@link #run()} method by invoking
 * the {@link #getRequestHandlerStatus()} method.
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
	 * @param socket the socket a request message is to be extracted from
	 * @param requestParserStrategy the parsing strategy
	 * @param callback the function {@link #run()} invokes after being completed
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
	 * @param socket the socket a request message is to be extracted from
	 * @param requestParserStrategy the parsing strategy
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
	 * Returns the execution status of {@link #run()}.
	 * @return the execution status of {@link #run()}
	 */
	public RequestHandler.Status getRequestHandlerStatus() {
		return status;
	}

	/**
	 * Returns the current MediaPool instance.
	 * @return the current MediaPool instance
	 */
	public MediaPool getMediaPool() {
		return pool;
	}

	/**
	 * Returns the current RubusSocket instance.
	 * @return the current RubusSocket instance
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
	 * Returns the current RequestParserStrategy instance.
	 * @return the current RequestParserStrategy instance
	 */
	public RequestParserStrategy getRequestParserStrategy() {
		return parser;
	}

	/**
	 * Sets a new MediaPool instance.
	 * @param newMediaPool a new MediaPool instance
	 */
	public void setMediaPool(MediaPool newMediaPool) {
		assert newMediaPool != null;

		pool = newMediaPool;
	}

	/**
	 * Sets a new RubusSocket instance.
	 * @param newRubusSocket a new RubusSocket instance
	 */
	public void setRubusSocket(RubusSocket newRubusSocket) {
		assert newRubusSocket != null;

		socket = newRubusSocket;
	}

	/**
	 * Sets a new RequestParserStrategy instance.
	 * @param newRequestParserStrategy a new RequestParserStrategy instance
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
	 * Defines the set of {@link #run()} execution results.
	 */
	public enum ExecutionStatus {
		/**
		 * The request was successfully processed.
		 */
		SUCCESS,

		/**
		 * The exception occurred while processing the request message ( e.g. timeout exception, I/O exception etc. )
		 */
		EXCEPTION
	}

	/**
	 * RequestHandler.Status allows the client to get the detailed information on {@link #run()} execution.
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