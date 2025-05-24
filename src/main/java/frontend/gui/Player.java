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

package frontend.gui;

import common.DecodingException;
import common.net.FetchingException;
import common.net.RubusException;
import common.net.response.body.MediaInfo;
import frontend.*;
import frontend.decoders.BMPDecoder;
import frontend.decoders.VideoDecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

public class Player extends JPanel implements PlayerInterface, ExceptionHandler {

	private final ArrayList<Observer> observers = new ArrayList<>();

	private int progress;

	private EncodedPlaybackPiece[] buffer = new EncodedPlaybackPiece[0];

	private boolean isPaused = false;

	private long lastFrameTime = 0;

	private long deviation = 0;

	private final int controlsHeight;

	private Rectangle pauseButtonBorders = new Rectangle();

	private Rectangle rewindBarBorders = new Rectangle();

	private final MediaInfo mi;

	private BMPDecoder currentSecondDecoder = null;

	private BMPDecoder nextSecondDecoder = null;

	private int frameCounter = 0;

	private EncodedPlaybackPiece playingPiece = null;

	private Exception occurredException = null;

	private boolean isBuffering = true;

	public Player(int initialProgress, MediaInfo mediaInfo) {
		assert initialProgress >= 0 && mediaInfo != null;

		mi = mediaInfo;
		setProgress(initialProgress);
		setBackground(Color.BLACK);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				controlsClickHandler(mouseEvent);
			}
		});

		int controlHeightPercent = 7;
		int controlHeightMax = 50;
		controlsHeight = (int) Math.min(getVideoHeight() / 100D * controlHeightPercent, controlHeightMax);

		Dimension dimension = new Dimension(getVideoWidth(), getVideoHeight() + controlsHeight);
		setSize(dimension);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setMaximumSize(dimension);
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
		return mi.duration();
	}

	@Override
	public int getVideoWidth() {
		return mi.videoWidth();
	}

	@Override
	public int getVideoHeight() {
		return mi.videoHeight();
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
	public EncodedPlaybackPiece getPlayingPiece() {
		return playingPiece;
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
		if (pauseButtonBorders.contains(me.getPoint())) {
			if (isPaused()) {
				deviation = 0;
				lastFrameTime = 0;
				resume();
			} else {
				pause();
			}
			sendNotification();
		} else if (rewindBarBorders.contains(me.getPoint())) {
			currentSecondDecoder = nextSecondDecoder = null;
			occurredException = null;
			deviation = 0;
			lastFrameTime = 0;
			isBuffering = true;
			int previousSecond = getProgress();
			double relativePosition =
				(double) (me.getX() - rewindBarBorders.x) / rewindBarBorders.width;
			setProgress((int) (relativePosition * getVideoDuration()));
			if (getProgress() > previousSecond && getProgress() - previousSecond < getBuffer().length) {
				int piecesToSkip = getProgress() - previousSecond;
				if (getPlayingPiece() != null) piecesToSkip--;
				setBuffer(Arrays.copyOfRange(
					getBuffer(),
					piecesToSkip,
					getBuffer().length
				));
			} else setBuffer(new EncodedPlaybackPiece[0]);
			playingPiece = null;
			sendNotification();
		}
	}

	private void drawFrame(Graphics g) {
		try {
			if (occurredException != null) throw occurredException;
			if (getBuffer().length == 0 && currentSecondDecoder == null) {
				deviation = 0;
				lastFrameTime = 0;
				return;
			}

			if (currentSecondDecoder == null) {
				playingPiece = getBuffer()[0];
				currentSecondDecoder = new BMPDecoder(mi.videoContainer(), false, playingPiece.video(), this);
				if (getBuffer().length > 1)
					nextSecondDecoder = new BMPDecoder(mi.videoContainer(), true, getBuffer()[1].video(), this);
				setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
				isBuffering = true;
				sendNotification();
				return;
			} else if (!currentSecondDecoder.isDone()) {
				return;
			} else if (isBuffering && currentSecondDecoder.isDone()) {
				isBuffering = false;
				sendNotification();
			}

			Image frame = currentSecondDecoder.getFrame(frameCounter);
			g.drawImage(frame, 0, 0, null);

			if (!isPaused() && System.nanoTime() - lastFrameTime >= currentSecondDecoder.framePaceNs() + deviation) {
				if (lastFrameTime != 0)
					deviation += currentSecondDecoder.framePaceNs() - (System.nanoTime() - lastFrameTime);
				lastFrameTime = System.nanoTime();
				frameCounter++;
				if (frameCounter == currentSecondDecoder.getTotalFrames()) {
					setProgress(getProgress() + 1);
					currentSecondDecoder = nextSecondDecoder;

					if (getBuffer().length > 1) {
						nextSecondDecoder =
							new BMPDecoder(
								mi.videoContainer(),
								!currentSecondDecoder.isReversed(),
								getBuffer()[1].video(),
								this
							);
						playingPiece = getBuffer()[0];
						setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
					} else if (getBuffer().length > 0) {
						nextSecondDecoder = null;
						playingPiece = getBuffer()[0];
						setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
					} else {
						playingPiece = null;
						isBuffering = true;
					}

					sendNotification();
				}
			}
		} catch (Exception e) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getVideoWidth(), getVideoHeight());
			g.setColor(Color.WHITE);
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 25));
			g.drawString(e.getClass().getName(), 0, g.getFontMetrics().getMaxAscent());
			if (e.getMessage() != null) {
				g.drawString(e.getMessage(), 0, g.getFontMetrics().getHeight() + g.getFontMetrics().getMaxAscent());
			}
		}
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
