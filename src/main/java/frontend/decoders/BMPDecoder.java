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

package frontend.decoders;

import common.DecodingException;
import frontend.ExceptionHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * This class is a temporary workaround ( i don't know how implement proper hardware decoding ) transcoding a video into
 * frames represented by bmp images, that are stored in the temporary directory. The transcoding is backed by ffmpeg
 * meaning it supports a variety of codecs and containers. The transcoding is obviously happens in a separate thread and
 * the time to transcode depends on the original video quality and the cpu performance. In other words, it's recommended
 * to have two decoders to run simultaneously to provide seamless video decoding.
 */
public class BMPDecoder implements Runnable {

	private final Path dir;

	private final boolean reversed;

	private final Thread thread;

	private final byte[] video;

	private int totalFrames;

	private final String container;

	private ExceptionHandler handler;

	private String ffmpegLocation = "";

	/**
	 * Constructs an instance of this class.
	 * @param containerFormat the video container
	 * @param reversed if true stores frames in rubus/a, false stores rames in rubus/b
	 * @param video the video piece
	 * @param handler an exception handler
	 * @param startImmediately true if to start decoding after this class is created
	 * @throws DecodingException if decoding exception occurs
	 */
	public BMPDecoder(
		String containerFormat, boolean reversed, byte[] video, ExceptionHandler handler, boolean startImmediately
	) throws DecodingException {
		assert containerFormat != null && video != null;

		try {
			this.reversed = reversed;
			container = containerFormat;
			this.video = video;
			this.handler = handler;
			Path framesPath = Path.of(System.getProperty("java.io.tmpdir"), "rubus");
			dir = !reversed ? Path.of(framesPath.toString(), "a") : Path.of(framesPath.toString(), "b");
			if (Files.notExists(dir)) Files.createDirectories(dir);

			thread = new Thread(this);
			if (startImmediately) thread.start();
		} catch (Exception e) {
			throw new DecodingException(e.getMessage());
		}
	}

	/**
	 * Constructs an instance of this class and immediately starts the decoding.
	 * @param containerFormat the video container
	 * @param reversed if true stores frames in rubus/a, false stores rames in rubus/b
	 * @param video the video piece
	 * @param handler an exception handler
	 * @throws DecodingException if decoding exception occurs
	 */
	public BMPDecoder(
		String containerFormat, boolean reversed, byte[] video, ExceptionHandler handler
	) throws DecodingException {
		this(containerFormat, reversed, video, handler, true);
	}

	/**
	 * Constructs an instance of this class without an exception handler and immediately starts the decoding.
	 * @param containerFormat the video container
	 * @param reversed if true stores frames in rubus/a, false stores rames in rubus/b
	 * @param video the video piece
	 * @throws DecodingException if decoding exception occurs
	 */
	public BMPDecoder(String containerFormat, boolean reversed, byte[] video) throws DecodingException {
		this(containerFormat, reversed, video, null);
	}

	/**
	 * Returns true if the directory is reversed, otherwise false.
	 * @return true if the directory is reversed, otherwise false
	 */
	public boolean isReversed() {
		return reversed;
	}

	@Override
	public void run() {
		try {
			Path videoPath = Path.of(dir.toString(), "video." + container);
			Files.write(videoPath, video);
			Runtime runtime = Runtime.getRuntime();
			String[] p1Command =
				new String[]{
					getFfmpegLocation() + "ffprobe",
					"-loglevel",
					"quiet",
					"-select_streams",
					"v:0",
					"-count_packets",
					"-show_entries",
					"stream=nb_read_packets",
					"-of",
					"csv=p=0",
					videoPath.toString()
				};
			Process p1 = runtime.exec(p1Command);
			p1.waitFor(1000, TimeUnit.MILLISECONDS);
			if (p1.exitValue() != 0)
				throw new DecodingException(String.join(" ", p1Command) + " exit code: " + p1.exitValue());
			InputStream is = p1.getInputStream();
			StringBuilder stringBuffer = new StringBuilder();
			int character;
			while ((character = is.read()) != -1) {
				stringBuffer.append(Character.toChars(character));
			}
			totalFrames = Integer.parseInt(stringBuffer.toString().trim());

			String[] p2Command =
				new String[]{
					getFfmpegLocation() + "ffmpeg",
					"-loglevel",
					"quiet",
					"-y",
					"-i",
					videoPath.toString(),
					dir.toString() + File.separator + "frame%d.bmp"
				};
			Process p2 = runtime.exec(p2Command);
			p2.waitFor(1000, TimeUnit.MILLISECONDS);
			if (p2.exitValue() != 0)
				throw new DecodingException(String.join(" ", p2Command) + " exit code: " + p2.exitValue());
		} catch (DecodingException e) {
			if (handler != null) handler.handleException(e);
		} catch (Exception e) {
			if (handler != null) handler.handleException(new DecodingException(e.getMessage()));
		}
	}

	/**
	 * Returns true if decoding is done.
	 * @return true if decoding is done
	 */
	public boolean isDone() {
		return !thread.isAlive();
	}

	/**
	 * Returns a frame from the encoded video.
	 * @param index the index of the frame
	 * @return an Image instance
	 * @throws DecodingException if decoding exception occurs
	 */
	public Image getFrame(int index) throws DecodingException {
		try {
			return ImageIO.read(Path.of(dir.toString(), "frame" + (index + 1) + ".bmp").toFile());
		} catch (IOException e) {
			throw new DecodingException(e.getMessage());
		}
	}

	/**
	 * Returns the number of frames in the encoded video.
	 * @return the number of frames in the encoded video
	 */
	public int getTotalFrames() {
		return totalFrames;
	}

	/**
	 * Returns the frame-pace in seconds.
	 * @return the frame-pace in seconds
	 */
	public double framePace() {
		return 1D / getTotalFrames();
	}

	/**
	 * Returns the frame-pace in nanoseconds.
	 * @return the frame-pace in nanoseconds
	 */
	public int framePaceNs() {
		return 1_000_000_000 / getTotalFrames();
	}

	/**
	 * Sets the location to ffmpeg.
	 * @param loc the location to ffmpeg
	 */
	public void setFfmpegLocation(String loc) {
		assert loc != null;
		if (loc.isEmpty() || loc.endsWith(File.separator)) ffmpegLocation = loc;
		else ffmpegLocation = ffmpegLocation + File.separator;
	}

	/**
	 * Returns the current location to ffmpeg.
	 * @return the current location to ffmpeg
	 */
	public String getFfmpegLocation() {
		return ffmpegLocation;
	}

	public void start() {
		thread.start();
	}

	/**
	 * Returns the current exception handler.<br><br>
	 *
	 * The passed exception:
	 *     {@link DecodingException} if decoding failed
	 * @return the current exception handler
	 */
	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	/**
	 * Sets a new exception handler.<br><br>
	 *
	 * The passed exception:
	 *     {@link DecodingException} if decoding failed
	 * @param handler a new exception handler
	 */
	public void setExceptionHandler(ExceptionHandler handler) {
		this.handler = handler;
	}
}
