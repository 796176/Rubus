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

package frontend.gui;

import common.DecodingException;
import common.net.FetchingException;
import common.net.RubusException;
import frontend.*;
import frontend.decoders.Decoder;
import frontend.decoders.VideoDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Player extends JPanel implements PlayerInterface, ExceptionHandler {

	private final Logger logger = LoggerFactory.getLogger(Player.class);

	private final ArrayList<Observer> observers = new ArrayList<>();

	private volatile int progress;

	private volatile EncodedPlaybackPiece[] buffer = new EncodedPlaybackPiece[0];

	private volatile boolean isPaused = false;

	private volatile long lastFrameTime = 0;

	private volatile long deviation = 0;

	private volatile int controlsHeight = 0;

	private Rectangle pauseButtonBorders = new Rectangle();

	private Rectangle rewindBarBorders = new Rectangle();

	private volatile int frameCounter = 0;

	private volatile EncodedPlaybackPiece playingPiece = null;

	private Exception occurredException = null;

	private volatile boolean isBuffering = true;

	private volatile int duration;

	private volatile VideoDecoder vd;

	private Decoder.StreamContext sc;

	private enum ScStatus {
		NOT_INITIATED, INITIATING, INITIATED
	}

	private volatile ScStatus streamContextStatus = ScStatus.NOT_INITIATED;

	private enum PreDecodingStatus {
		NOT_PRE_DECODED, DECODING, PRE_DECODED
	}

	private volatile PreDecodingStatus preDecodingStatus = PreDecodingStatus.NOT_PRE_DECODED;

	private final Lock renderLock = new ReentrantLock();

	public Player(int initialProgress, VideoDecoder videoDecoder, int duration) {
		assert initialProgress >= 0;

		setDecoder(videoDecoder);
		setVideoDuration(duration);
		setProgress(initialProgress);
		setBackground(Color.BLACK);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				controlsClickHandler(mouseEvent);
			}
		});

		logger.debug(
			"{} instantiated, initial progress: {}, VideoDecoder: {}, duration: {}",
			this,
			initialProgress,
			videoDecoder,
			duration
		);
	}

	@Override
	public void close() throws Exception {
		renderLock.lock();
		try {
			switch (preDecodingStatus) {
				case DECODING -> {
					if (vd.getStreamContextNow() != null) vd.getStreamContextNow().close();
				}
				case PRE_DECODED -> sc.close();
			}
		} finally {
			renderLock.unlock();
		}

		logger.debug("{} closed", this);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		drawFrame(g);
		drawControls(g);
	}

	@Override
	public void pause() {
		isPaused = true;
	}

	@Override
	public void resume() {
		isPaused = false;
	}

	@Override
	public boolean isPaused() {
		return isPaused;
	}

	@Override
	public int getProgress() {
		return progress;
	}

	@Override
	public int getVideoDuration() {
		return duration;
	}

	@Override
	public void setVideoDuration(int newDuration) {
		assert newDuration > 0;

		duration = newDuration;
	}

	@Override
	public int getVideoWidth() {
		return 0;
	}

	@Override
	public int getVideoHeight() {
		return 0;
	}

	@Override
	public void setProgress(int timestamp) {
		assert timestamp <= getVideoDuration();

		progress = timestamp;
		frameCounter = 0;
	}

	@Override
	public EncodedPlaybackPiece[] getBuffer() {
		return buffer;
	}

	@Override
	public void setBuffer(EncodedPlaybackPiece[] buffer) {
		assert buffer != null;

		this.buffer = buffer;
	}

	@Override
	public boolean isBuffering() {
		return isBuffering;
	}

	@Override
	public VideoDecoder getDecoder() {
		return null;
	}

	@Override
	public void setDecoder(VideoDecoder videoDecoder) {
		assert videoDecoder != null;

		vd = videoDecoder;
	}

	@Override
	public EncodedPlaybackPiece getPlayingPiece() {
		return playingPiece;
	}

	public void purge() throws Exception {
		renderLock.lock();
		try {
			observers.clear();
			close();
			streamContextStatus = ScStatus.NOT_INITIATED;
			preDecodingStatus = PreDecodingStatus.NOT_PRE_DECODED;
			isBuffering = true;
			deviation = 0;
			lastFrameTime = 0;
			isPaused = false;
			controlsHeight = 0;
			frameCounter = 0;
			duration = 0;
			setBuffer(new EncodedPlaybackPiece[0]);
			playingPiece = null;
		} finally {
			renderLock.unlock();
		}
	}

	@Override
	public void attach(Observer o) {
		assert o != null;

		observers.add(o);
	}

	@Override
	public void detach(Observer o) {
		observers.remove(o);
	}

	@Override
	public Observer[] getObservers() {
		return observers.toArray(new Observer[0]);
	}

	@Override
	public void sendNotification() {
		for (Observer o: getObservers()) {
			o.update(this);
		}
	}

	private void drawControls(Graphics g) {
		int controlHeightPercent = 7;
		int controlHeightMax = 50;
		controlsHeight = (int) Math.min(getHeight() / 100D * controlHeightPercent, controlHeightMax);

		int elementOffset = (int) (controlsHeight / 100.0D * 15);
		g.setColor(Color.DARK_GRAY);
		g.fillRect(0, getHeight() - controlsHeight, getWidth(), controlsHeight);

		pauseButtonBorders =
			new Rectangle(
				elementOffset,
				getHeight() - controlsHeight + elementOffset,
				controlsHeight - elementOffset * 2,
				controlsHeight - elementOffset * 2
			);
		g.setColor(Color.WHITE);
		if (isPaused()) {
			int x1 = pauseButtonBorders.x;
			int x2 = pauseButtonBorders.x;
			int x3 = pauseButtonBorders.x + pauseButtonBorders.width;
			int y1 = pauseButtonBorders.y;
			int y2 = pauseButtonBorders.y + pauseButtonBorders.height;
			int y3 = pauseButtonBorders.y + pauseButtonBorders.height / 2;
			g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
		} else {
			g.fillRect(
				pauseButtonBorders.x,
				pauseButtonBorders.y,
				pauseButtonBorders.width / 3,
				pauseButtonBorders.height
			);
			g.fillRect(
				pauseButtonBorders.x + 2 * pauseButtonBorders.width / 3,
				pauseButtonBorders.y,
				pauseButtonBorders.width / 3,
				pauseButtonBorders.height
			);
		}

		rewindBarBorders = new Rectangle(
			2 * elementOffset + pauseButtonBorders.width,
			getHeight() - controlsHeight + elementOffset,
			getWidth() - 3 * elementOffset - pauseButtonBorders.width,
			controlsHeight - 2 * elementOffset
		);
		g.drawRect(
			rewindBarBorders.x,
			rewindBarBorders.y,
			rewindBarBorders.width,
			rewindBarBorders.height
		);

		g.setColor(Color.BLUE);
		g.fillRect(
			rewindBarBorders.x,
			rewindBarBorders.y,
			(int) (rewindBarBorders.width * ((double) getProgress() / getVideoDuration())),
			rewindBarBorders.height
		);
	}

	private void controlsClickHandler(MouseEvent me) {
		if (renderLock.tryLock()) {
			try {
				if (pauseButtonBorders.contains(me.getPoint())) {
					if (isPaused()) {
						deviation = 0;
						lastFrameTime = 0;
						resume();
						logger.debug("{} resumed", this);
					} else {
						pause();
						logger.debug("{} paused", this);
					}
					sendNotification();
				} else if (rewindBarBorders.contains(me.getPoint())) {
					vd.purge();
					preDecodingStatus = PreDecodingStatus.NOT_PRE_DECODED;
					occurredException = null;
					deviation = 0;
					lastFrameTime = 0;
					isBuffering = true;
					int previousProgress = getProgress();
					double relativePosition =
						(double) (me.getX() - rewindBarBorders.x) / rewindBarBorders.width;
					int newProgress = (int) (relativePosition * getVideoDuration());
					setProgress(newProgress);
					if (getProgress() > previousProgress && getProgress() - previousProgress < getBuffer().length) {
						int piecesToSkip = getProgress() - previousProgress;
						if (getPlayingPiece() != null) piecesToSkip--;
						setBuffer(Arrays.copyOfRange(
							getBuffer(),
							piecesToSkip,
							getBuffer().length
						));
					} else setBuffer(new EncodedPlaybackPiece[0]);
					playingPiece = null;
					sendNotification();

					logger.debug("{}'s progress reassigned from {} to {}", this, previousProgress, newProgress);
				}
			} finally {
				renderLock.unlock();
			}
		}
	}

	private void drawFrame(Graphics g) {
		if (!renderLock.tryLock()) return;
		try {
			if (occurredException != null) throw occurredException;
			if (getBuffer().length == 0 && preDecodingStatus == PreDecodingStatus.NOT_PRE_DECODED) {
				deviation = 0;
				lastFrameTime = 0;
				return;
			}

			if (streamContextStatus == ScStatus.NOT_INITIATED) {
				vd.startStreamContextInitialization(getBuffer()[0].video());
				streamContextStatus = ScStatus.INITIATING;
				return;
			} else if (
				streamContextStatus == ScStatus.INITIATING &&
				vd.getStreamContext() == null &&
				vd.getStreamContextInitializationException() == null
			) {
				return;
			} else if (
				streamContextStatus == ScStatus.INITIATING &&
				(vd.getStreamContext() != null || vd.getStreamContextInitializationException() != null)
			) {
				if (vd.getStreamContextInitializationException() != null) {
					throw vd.getStreamContextInitializationException();
				}
				sc = vd.getStreamContext();
				streamContextStatus = ScStatus.INITIATED;
			}

			if (preDecodingStatus == PreDecodingStatus.NOT_PRE_DECODED) {
				vd.startDecodingOfAllFrames(getProgress(), sc, getBuffer()[0].video());
				if (getBuffer().length > 1) {
					vd.startDecodingOfAllFrames(getProgress() + 1, sc, getBuffer()[1].video());
				}
				playingPiece = getBuffer()[0];
				setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
				preDecodingStatus = PreDecodingStatus.DECODING;
				sendNotification();
				return;
			} else if (preDecodingStatus == PreDecodingStatus.DECODING && !vd.isDecodingComplete(getProgress())) {
				return;
			} else if (preDecodingStatus == PreDecodingStatus.DECODING && vd.isDecodingComplete(getProgress())) {
				preDecodingStatus = PreDecodingStatus.PRE_DECODED;
				isBuffering = false;
				sendNotification();
			}

			if (vd.getDecodingException(getProgress()) != null) throw vd.getDecodingException(getProgress());
			Image frame = vd.getDecodedFrames(getProgress()).frames()[frameCounter];

			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHints(
				new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
			);

			int availableH = getHeight() - controlsHeight;
			int availableW = getWidth();
			double widthDif = ((double)availableW / frame.getWidth(null));
			double heightDif =((double)availableH / frame.getHeight(null));
			double scale = Math.min(widthDif, heightDif);
			int renderingWidth = (int)(frame.getWidth(null) * scale);
			int renderingHeight = (int)(frame.getHeight(null) * scale);
			int xPoint = (availableW - renderingWidth) / 2;
			int yPoint = (availableH - renderingHeight) / 2;
			g.drawImage(frame, xPoint, yPoint, renderingWidth, renderingHeight, null);

			if (!isPaused() && System.nanoTime() - lastFrameTime >= vd.framePaceNs(sc) + deviation) {
				if (lastFrameTime != 0)
					deviation += vd.framePaceNs(sc) - (System.nanoTime() - lastFrameTime);
				lastFrameTime = System.nanoTime();
				frameCounter++;
				if (frameCounter == vd.getFrameRate(sc)) {
					vd.freeDecodedFrames(getProgress());
					setProgress(getProgress() + 1);
					if (getBuffer().length > 1) {
						playingPiece = getBuffer()[0];
						vd.startDecodingOfAllFrames(getProgress() + 1, sc, getBuffer()[1].video());
						setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
					} else if (getBuffer().length > 0) {
						playingPiece = getBuffer()[0];
						setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
					} else {
						playingPiece = null;
						isBuffering = true;
						preDecodingStatus = PreDecodingStatus.NOT_PRE_DECODED;
					}
					sendNotification();
				}
			}
		} catch (Exception e) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight() - controlsHeight);
			g.setColor(Color.WHITE);
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 25));
			g.drawString(e.getClass().getName(), 0, g.getFontMetrics().getMaxAscent());
			if (e.getMessage() != null) {
				g.drawString(e.getMessage(), 0, g.getFontMetrics().getHeight() + g.getFontMetrics().getMaxAscent());
			}
		}
		renderLock.unlock();
	}


	@Override
	public void handleException(Exception e) {
		if (e instanceof FetchingException fetchingException) {
			if (getBuffer().length == 0 && isBuffering())
				occurredException = fetchingException;
		} else if (e instanceof RubusException rubusException) {
			if (getBuffer().length == 0 && isBuffering()) {
				occurredException = rubusException;
			}
		} else if (e instanceof DecodingException decodingException) {
			occurredException = decodingException;
		}
	}
}
