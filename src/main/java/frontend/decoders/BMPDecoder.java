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

public class BMPDecoder implements Runnable {

	private final Path dir;

	private final boolean reversed;

	private final Thread thread;

	private final byte[] video;

	private int totalFrames;

	private final String container;

	private ExceptionHandler handler;

	private String ffmpegLocation = "";

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

	public BMPDecoder(
		String containerFormat, boolean reversed, byte[] video, ExceptionHandler handler
	) throws DecodingException {
		this(containerFormat, reversed, video, handler, true);
	}

	public BMPDecoder(String containerFormat, boolean reversed, byte[] video) throws DecodingException {
		this(containerFormat, reversed, video, null);
	}

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

	public boolean isDone() {
		return !thread.isAlive();
	}

	public Image getFrame(int index) throws DecodingException {
		try {
			return ImageIO.read(Path.of(dir.toString(), "frame" + (index + 1) + ".bmp").toFile());
		} catch (IOException e) {
			throw new DecodingException(e.getMessage());
		}
	}

	public int getTotalFrames() {
		return totalFrames;
	}

	public double framePace() {
		return 1D / getTotalFrames();
	}

	public int framePaceNs() {
		return 1_000_000_000 / getTotalFrames();
	}

	public void setFfmpegLocation(String loc) {
		assert loc != null;
		if (loc.isEmpty() || loc.endsWith(File.separator)) ffmpegLocation = loc;
		else ffmpegLocation = ffmpegLocation + File.separator;
	}

	public String getFfmpegLocation() {
		return ffmpegLocation;
	}

	public void start() {
		thread.start();
	}

	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	public void setExceptionHandler(ExceptionHandler handler) {
		this.handler = handler;
	}
}
