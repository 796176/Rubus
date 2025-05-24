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

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * FfmpegJniVideoDecoder is an implementation of {@link Decoder} that targets decoding of video clips consisting of (1)
 * a single video stream itself and (2) a container that contains necessary information to decode and output the content
 * ( e.g. the resolution, frame-rate, etc. ). The range of supported containers and video decoders is platform specific
 * and depends on the version of the ffmpeg library.<br>
 * This class doesn't perform decoding in Java but instead delegates it to the functions declared in
 * src/main/c/fronted/decoders/fronted_decoders_FfmpegJniVideoDecoder.h and implemented in
 * src/main/c/fronted/decoders/fronted_decoders_FfmpegJniVideoDecoder.c. They, in turn, call functions provided by
 * the ffmpeg library. That means the target platform has to have the ffmpeg library installed and have the platform
 * specific binary of the C code placed somewhere. After the location of the binary is specified in the
 * java.library.path system property, it can be loaded via {@link System#loadLibrary(String)} using platform
 * independent name.<br>
 * This class is quite limited because it doesn't provide support for {@link Decoder.LocalContext} and parallel
 * decoding. LocalContext or StreamContext instances used here has to be instantiated by the respective methods
 * implemented by this class. Using a LocalContext/StreamContext instance with a video clip that is not intended to be
 * used with that particular instance can result in a program crash, so the client needs to use contexts with extreme
 * caution.
 */
public class FfmpegJniVideoDecoder extends VideoDecoder {

	private native Object[] decodeFrames(
		long contextAddress, int contextType, byte[] encodedVideo, int offset, int total
	);

	private native int frames(long contextAddress, int contextType);

	private native long initContext(byte[] vid, int contextType);

	private native void freeContext(long contextAddress, int contextType);


	private final Map<Integer, Future<DecodedFrames>> decodingStatuses = new HashMap<>();

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	private Future<Decoder.StreamContext> streamContextFuture = null;

	private LocalContext localContext = null;

	@Override
	public void startDecodingOfAllFrames(int id, StreamContext streamContext, byte[] media) {
		assert streamContext instanceof StreamContextImpl && !streamContext.isClosed() && media != null;

		Future<DecodedFrames> future = executorService.submit(() -> {
			StreamContextImpl streamContextImpl = (StreamContextImpl) streamContext;
			Object[] frames = decodeFrames(
				streamContextImpl.getStreamContextMemoryAddress(),
				0,
				media,
				0,
				frames(streamContextImpl.getStreamContextMemoryAddress(), 0)
			);
			return new DecodedFrames((Image[]) frames, 0);
		});
		decodingStatuses.put(id, future);
	}

	/**
	 * Delegates the call to {@link #startDecodingOfAllFrames(int, StreamContext, byte[])}, so it's equivalent to
	 * startDecodingOfAllFrames(id, localContext.getStreamContext, media)
	 */
	@Override
	public void startDecodingOfNFrames(int id, LocalContext localContext, byte[] media, int offset, int total) {
		assert
			localContext instanceof LocalContextImpl &&
			!localContext.getStreamContext().isClosed() &&
			media != null &&
			total >= 0 &&
			offset + total <= getFrameRate(localContext.getStreamContext());

		startDecodingOfAllFrames(id, localContext.getStreamContext(), media);
	}

	@Override
	public DecodedFrames getDecodedFrames(int id) {
		if (!isDecodingComplete(id)) return null;
		try {
			return decodingStatuses.get(id).get();
		} catch (Exception ignored) { }
		return null;
	}

	@Override
	public DecodedFrames getDecodedFramesNow(int id) throws InterruptedException {
		if (!decodingStatuses.containsKey(id)) return null;
		try {
			return decodingStatuses.get(id).get();
		} catch (ExecutionException ignored) { }
		return null;
	}

	@Override
	public boolean isDecodingComplete(int id) {
		if (!decodingStatuses.containsKey(id)) return false;
		return decodingStatuses.get(id).isDone();
	}

	@Override
	public Exception getDecodingException(int id) {
		if (!isDecodingComplete(id)) return null;
		try {
			decodingStatuses.get(id).get();
		} catch (ExecutionException e) {
			return (Exception) e.getCause();
		} catch (InterruptedException ignored) { }
		return null;
	}

	@Override
	public void freeDecodedFrames(int id) {
		decodingStatuses.remove(id);
	}

	@Override
	public int getFrameRate(StreamContext streamContext) {
		assert streamContext instanceof StreamContextImpl && !streamContext.isClosed();

		return frames(((StreamContextImpl) streamContext).getStreamContextMemoryAddress(), 0);
	}

	@Override
	public long framePaceMs(StreamContext streamContext) {
		return 1000L / getFrameRate(streamContext);
	}

	@Override
	public long framePaceNs(StreamContext streamContext) {
		return 1_000_000_000L / getFrameRate(streamContext);
	}

	@Override
	public void startStreamContextInitialization(byte[] media) {
		assert media != null;

		streamContextFuture = executorService.submit(() -> new StreamContextImpl(initContext(media, 0)));
	}

	@Override
	public StreamContext getStreamContext() {
		if (streamContextFuture == null || !streamContextFuture.isDone()) return null;
		try {
			return streamContextFuture.get();
		} catch (Exception ignored) { }
		return null;
	}

	@Override
	public StreamContext getStreamContextNow() throws InterruptedException {
		if (streamContextFuture == null) return null;
		try {
			return streamContextFuture.get();
		} catch (ExecutionException ignored) { }
		return null;
	}

	@Override
	public Exception getStreamContextInitializationException() {
		if (streamContextFuture == null || !streamContextFuture.isDone()) return null;
		try {
			streamContextFuture.get();
		} catch (ExecutionException e) {
			return (Exception) e.getCause();
		} catch (InterruptedException ignored) { }
		return null;
	}

	@Override
	public void startLocalContextInitialization(byte[] media, StreamContext streamContext) {
		assert streamContext instanceof StreamContextImpl && !streamContext.isClosed();

		localContext = new LocalContextImpl((StreamContextImpl) streamContext);
	}

	@Override
	public LocalContext getLocalContext() {
		return localContext;
	}

	@Override
	public LocalContext getLocalContextNow() {
		return localContext;
	}

	@Override
	public Exception getLocalContextInitializationException() {
		return null;
	}

	@Override
	public void purge() {
		decodingStatuses.forEach((i, f) -> f.cancel(false));
		decodingStatuses.clear();
		localContext = null;
		streamContextFuture = null;
	}

	@Override
	public void close() throws Exception {
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	private class StreamContextImpl implements StreamContext {

		private final long memoryAddress;

		private boolean isClosed = false;

		private long getStreamContextMemoryAddress() {
			return memoryAddress;
		}

		private StreamContextImpl(long memoryAddress) {
			this.memoryAddress = memoryAddress;
		}

		@Override
		public void close() {
			if (isClosed()) return;
			freeContext(getStreamContextMemoryAddress(), 0);
			isClosed = true;
		}

		@Override
		public boolean isClosed() {
			return isClosed;
		}
	}

	private class LocalContextImpl implements LocalContext {

		private final StreamContext streamContext;

		private LocalContextImpl(StreamContextImpl streamContext) {
			assert streamContext != null;

			this.streamContext = streamContext;
		}

		@Override
		public StreamContext getStreamContext() {
			return streamContext;
		}

		@Override
		public boolean isClosed() {
			return true;
		}

		@Override
		public void close() { }
	}
}
