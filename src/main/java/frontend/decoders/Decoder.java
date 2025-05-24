/*
 * Rubus is an application level protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024 Yegore Vlussove
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

/**
 * Decoder provides an interface the client uses to decode an abstract entity, which, for example, can be video or audio
 * stored in a container. The implementations need to specify the data structure that describes decoded frames, which
 * will available for the client to use. The Decoder interface is designed to be used in a time sensitive environment,
 * so all methods - unless otherwise is specified - are non-blocking and don't take long to return control to
 * the caller. That, in turn, means that actual decoding happens in a separate thread/threads where the level of
 * parallelism is up to the implementations.<br>
 * Commonly, decoding works in a flow-like manner: after one entity has been decoded, the next entity needs to be
 * decoded immediately after. To accommodate this design Decoder associates an integer with the encoded entity that is
 * used for successive operations related to the entity. That allows the user to queue decoding of several entities.
 * Think of Decoder as a hash-map, where the integer is a key, which can be used to retrieve the decoded frames or
 * inquire if the decoding of that particular entity is complete.<br>
 * Concrete implementations of Decoder may also want to implement the {@link StreamContext} and {@link LocalContext}
 * interfaces to accelerate decoding process. {@link StreamContext} may be used to store general information shared
 * across several entities, so they can be viewed as part of a stream. For example if the entity is a small video clip
 * and those video clips constitute a single video that has a certain frame-rate, resolution, container type, codec,
 * etc., these properties are shared across the video clips. {@link LocalContext} on the other hand stores information
 * related specifically to that entity. It can be used to perform several operation on that entity like
 * {@link #startDecodingOfNFrames(int, LocalContext, byte[], int, int)}.<br><br>
 *
 * Here is the scenario of how Decoder can be used:
 * <pre>
 * Decoder&#60;Frames&#62; decoder =...;
 * decoder.startStreamContextInitialization(encodedEntity);
 * // while the stream context is being initialized perform some tasks
 * ...
 * Decoder.StreamContext sc = decoder.getStreamContextNow(); // calling a blocking method to get the stream context
 * // check if the stream context initialization succeeded
 * if (decoder.getStreamContextInitializationException() != null) {
 *     decoder.close();
 *     throw new Exception();
 * }
 * // perform decoding of several entities at a time
 * decoder.startDecodingOfAllFrames(0, sc, encodedEntity);
 * decoder.startDecodingOfAllFrames(1, sc, anotherEncodedEntity);
 * // receive the result using blocking methods
 * Frames decodedFrames0 = decoder.getDecodedFramesNow(0);
 * Frames decodedFrames1 = decoder.getDecodedFramesNow(1);
 * // check if the decoding succeeded
 * if (decoder.getDecodingException(0) != null || decoder.getDecodingException(1) != null) {
 *     sc.close();
 *     decoder.close();
 *     throw new Exception();
 * }
 * // removing decoded entities from decoder so after decodedData0 and decodedData1 are used the decoded data can be
 * // garbage collected
 * decoder.freeDecodedFrames(0);
 * decoder.freeDecodedFrames(1);
 * // more calls to Decoder
 * ...
 * // closing resources after all necessary decoding is done
 * sc.close();
 * decoder.close();
 * </pre>
 *
 * @param <T> the data structure that describes decoded frames
 */
public interface Decoder <T> extends AutoCloseable {

	/**
	 * Begins decoding of all frames of the entity. After decoding is complete the decoded frames are retrieved
	 * via {@link #getDecodedFrames(int)} or {@link #getDecodedFramesNow(int)}. If an exception occurred while decoding
	 * it can be retrieved via {@link #getDecodingException(int)}.
	 * @param id the id of the entity
	 * @param streamContext the stream context
	 * @param media the entity
	 */
	void startDecodingOfAllFrames(int id, StreamContext streamContext, byte[] media);

	/**
	 * Begins decoding of frames of the entity within the range. The actual number of decoded frames may exceed
	 * the range if it's a more efficient way to do decoding; refer to concrete implementations for more details. After
	 * decoding is complete the decoded frames are retrieved via {@link #getDecodedFrames(int)} or
	 * {@link #getDecodedFramesNow(int)}. If an exception occurred while decoding it can be retrieved via
	 * {@link #getDecodingException(int)}.
	 * @param id the id of the entity
	 * @param localContext the local context
	 * @param media the entity
	 * @param offset number of frames to skip
	 * @param total number of frames to decode
	 */
	void startDecodingOfNFrames(int id, LocalContext localContext, byte[] media, int offset, int total);

	/**
	 * Returns decoded frames if the decoding is complete. If the decoding is not yet complete, or it hasn't
	 * been started via {@link #startDecodingOfAllFrames(int, StreamContext, byte[])} or
	 * {@link #startDecodingOfNFrames(int, LocalContext, byte[], int, int)}, or an exception occurred while decoding,
	 * null is returned.
	 * @param id the entity id
	 * @return decoded frames if decoding is complete, null otherwise
	 */
	T getDecodedFrames(int id);

	/**
	 * Returns decoded frames if the decoding is complete or blocks until it is. If the decoding hasn't been started
	 * via {@link #startDecodingOfAllFrames(int, StreamContext, byte[])} or
	 * {@link #startDecodingOfNFrames(int, LocalContext, byte[], int, int)}, or an exception occurred while decoding,
	 * null is returned.
	 * @param id the entity id
	 * @return decoded frames if the decoding went properly, null otherwise
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 */
	T getDecodedFramesNow(int id) throws InterruptedException;

	/**
	 * Returns true if the decoding went properly or an exception occurred, false otherwise.
	 * @param id the entity id
	 * @return true if the decoding went properly or an exception occurred, false otherwise
	 */
	boolean isDecodingComplete(int id);

	/**
	 * Returns an exception that occurred during decoding, null otherwise.
	 * @param id the entity id
	 * @return an exception that occurred during decoding, null otherwise
	 */
	Exception getDecodingException(int id);

	/**
	 * Removes decoded frames from the underlying data structure so they are can't be received via
	 * {@link #getDecodedFrames(int)} or {@link #getDecodedFramesNow(int)}. The client is encouraged to call this method
	 * after decoded frames are retrieved, so they can be garbage collected. Otherwise, the underlying data structure
	 * would keep references to all the decoded frames and this, in turn, can cause JVM to run out of memory.
	 * @param id the entity id
	 */
	void freeDecodedFrames(int id);

	/**
	 * Returns the number of frames in a second.
	 * @param streamContext the stream context
	 * @return the number of frames in a second
	 */
	int getFrameRate(StreamContext streamContext);

	/**
	 * Returns the duration of a single frame in milliseconds.
	 * @param streamContext the stream context
	 * @return the duration of a single frame in milliseconds
	 */
	long framePaceMs(StreamContext streamContext);

	/**
	 * Returns the duration of a single frame in nanoseconds.
	 * @param streamContext the stream context
	 * @return the duration of a single frame in nanoseconds
	 */
	long framePaceNs(StreamContext streamContext);

	/**
	 * Begins initialization of a stream context. After the initialization is complete the result can be retrieved via
	 * {@link #getStreamContext()} or {@link #getStreamContextNow()}. If an exception occurred during the initialization
	 * it can be retrieved via {@link #getStreamContextInitializationException()}.
	 * @param media an encoded entity containing properties shared across all entities of a stream
	 */
	void startStreamContextInitialization(byte[] media);

	/**
	 * Returns the stream context if the initialization is complete. If the initialization is not yet complete, or
	 * hasn't been started via {@link #startStreamContextInitialization(byte[])}, or an exception occurred during
	 * the initialization, null is returned.
	 * @return the stream context if the initialization is complete, null otherwise
	 */
	StreamContext getStreamContext();

	/**
	 * Returns the stream context if the initialization complete or blocks until it is. If the initialization hasn't
	 * been started via {@link #startStreamContextInitialization(byte[])}, or an exception occurred during
	 * the initialization, null is returned.
	 * @return the stream context if the initialization went properly, null otherwise
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 */
	StreamContext getStreamContextNow() throws InterruptedException;

	/**
	 * Returns an exception that occurred during the stream context initialization or null if it didn't.
	 * @return an exception that occurred during the initialization
	 */
	Exception getStreamContextInitializationException();

	/**
	 * Begins initialization of a local context. After the initialization is complete the result can be retrieved via
	 * {@link #getLocalContext()} or {@link #getLocalContextNow()}. If an exception occurred during the initialization
	 * it can be retrieved via {@link #getLocalContextInitializationException()}.
	 * @param media an encoded entity containing properties unique to that entity
	 * @param streamContext the stream context of the specified entity
	 */
	void startLocalContextInitialization(byte[] media, StreamContext streamContext);

	/**
	 * Returns the local context if the initialization is complete. If the initialization is not yet complete, or it
	 * hasn't been started via {@link #startLocalContextInitialization(byte[], StreamContext)}, or an exception occurred
	 * during the initialization, null is returned.
	 * @return the local context if the initialization is complete, null otherwise
	 */
	LocalContext getLocalContext();

	/**
	 * Returns the local context if the initialization is complete or blocks until it is. If the initialization hasn't
	 * been started via {@link #startLocalContextInitialization(byte[], StreamContext)}, or an exception occurred during
	 * the initialization, null is returned.
	 * @return the local context if the initialization went properly, null otherwise.
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 */
	LocalContext getLocalContextNow() throws InterruptedException;

	/**
	 * Returns an exception that occurred during the local context initialization or null if it didn't.
	 * @return an exception that occurred during the initialization
	 */
	Exception getLocalContextInitializationException();

	/**
	 * Reset the state of this Decoder to the initial state of the instantiation. It includes cancellation of
	 * scheduled decoding, removal of the decoded frames, and removal/cancellation of context initializations.
	 */
	void purge();

	/**
	 * StreamContext is used to accelerate the decoding process of more than one encoded entities that share common
	 * properties. StreamContext is meant to be used internally by a concrete implementation of Decoder, so it doesn't
	 * declare any implementation specific methods. If a concrete implementation doesn't want to use StreamContext it
	 * still has to implement the interface. The Decoder implementation doesn't need to manage the lifecycle of
	 * StreamContext: if the client doesn't need it anymore it must close it itself.
	 */
	interface StreamContext extends AutoCloseable {

		/**
		 * Returns true if this StreamContext is closed, false otherwise.
		 * @return true if this StreamContext is closed, false otherwise
		 */
		boolean isClosed();
	}

	/**
	 * LocalContext is used to accelerate the multi-stage decoding operations of a single encoded entity. That means a
	 * LocalContext is bound to a certain encoded entity and using this LocalContext with a different encoded entity
	 * would likely result in an error. LocalContext stores a reference to a StreamContext, so it doesn't need to store
	 * or process stream related properties, and it can focus on processing and storing properties unique to that
	 * entity. LocalContext is meant to be used internally by a concrete implementation, so it doesn't declare any
	 * implementation specific methods. If a concrete implementation doesn't want to use LocalContext it still has to
	 * implement the interface. The Decoder implementation doesn't need to manage the lifecycle of LocalContext: if
	 * the client doesn't need it anymore it must close it itself.
	 */
	interface LocalContext extends StreamContext {

		/**
		 * Returns the stream context of this LocalContext.
		 * @return the stream context of this LocalContext
		 */
		StreamContext getStreamContext();

		/**
		 * Returns true if this LocalContext is closed, false otherwise.
		 * @return true if this LocalContext is closed, false otherwise
		 */
		boolean isClosed();

		/**
		 * Closes this LocalContext; the underlying StreamContext needs to be closed separately.
		 * @throws Exception if this resource cannot be closed
		 */
		@Override
		void close() throws Exception;
	}
}
