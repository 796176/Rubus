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

package frontend.gui;

import common.DecodingException;
import common.net.FetchingException;
import common.net.RubusException;
import common.net.response.body.PlaybackInfo;
import frontend.*;
import frontend.decoders.BMPDecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

public class Player extends JPanel implements PlayerInterface, Subject, ExceptionHandler {

	private final ArrayList<Observer> observers = new ArrayList<>();

	private int currentSecond;

	private EncodedPlaybackPiece[] buffer = new EncodedPlaybackPiece[0];

	private boolean isPaused = false;

	private long lastFrameTime = 0;

	private long deviation = 0;

	private final int controlsHeight;

	private Rectangle pauseButtonBorders = new Rectangle();

	private Rectangle rewindBarBorders = new Rectangle();

	private final PlaybackInfo pi;

	private BMPDecoder currentSecondDecoder = null;

	private BMPDecoder nextSecondDecoder = null;

	private int frameCounter = 0;

	private EncodedPlaybackPiece playingPiece = null;

	private Exception occurredException = null;

	public Player(int startingTimestamp, PlaybackInfo playbackInfo) {
		assert startingTimestamp >= 0 && playbackInfo != null;

		pi = playbackInfo;
		setPlayingSecond(startingTimestamp);
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
	public int getCurrentSecond() {
		return currentSecond;
	}

	@Override
	public int getVideoDuration() {
		return pi.duration();
	}

	@Override
	public int getVideoWidth() {
		return pi.videoWidth();
	}

	@Override
	public int getVideoHeight() {
		return pi.videoHeight();
	}

	@Override
	public void setPlayingSecond(int timestamp) {
		assert timestamp <= getVideoDuration();

		currentSecond = timestamp;
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
		return playingPiece == null;
	}

	@Override
	public Decoder getDecoder() {
		return null;
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
			(int) (rewindBarBorders.width * ((double) getCurrentSecond() / getVideoDuration())),
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
			playingPiece = null;
			currentSecondDecoder = nextSecondDecoder = null;
			occurredException = null;
			deviation = 0;
			lastFrameTime = 0;
			long previousSecond = getCurrentSecond();
			double relativePosition =
				(double) (me.getX() - rewindBarBorders.x) / rewindBarBorders.width;
			setPlayingSecond((int) (relativePosition * getVideoDuration()));
			if (getCurrentSecond() > previousSecond && getCurrentSecond() - previousSecond < getBuffer().length) {
				setBuffer(Arrays.copyOfRange(
					getBuffer(),
					getBuffer().length - (int) (getCurrentSecond() - previousSecond),
					getBuffer().length
				));
			} else setBuffer(new EncodedPlaybackPiece[0]);
			sendNotification();
		}
	}

	private void drawFrame(Graphics g) {
		try {
			if (occurredException != null) throw occurredException;
			if (getBuffer().length == 0 && isBuffering()) return;

			if (currentSecondDecoder == null) {
				playingPiece = getBuffer()[0];
				currentSecondDecoder = new BMPDecoder(pi.videoContainer(), false, playingPiece.video(), this);
				if (getBuffer().length > 1)
					nextSecondDecoder = new BMPDecoder(pi.videoContainer(), true, getBuffer()[1].video(), this);
				setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
				sendNotification();
				return;
			} else if (!currentSecondDecoder.isDone()) return;

			Image frame = currentSecondDecoder.getFrame(frameCounter);
			g.drawImage(frame, 0, 0, null);

			if (!isPaused() && System.nanoTime() - lastFrameTime >= currentSecondDecoder.framePaceNs() + deviation) {
				if (lastFrameTime != 0)
					deviation += currentSecondDecoder.framePaceNs() - (System.nanoTime() - lastFrameTime);
				lastFrameTime = System.nanoTime();
				frameCounter++;
				if (frameCounter == currentSecondDecoder.getTotalFrames()) {
					setPlayingSecond(getCurrentSecond() + 1);
					currentSecondDecoder = nextSecondDecoder;

					if (getBuffer().length > 1) {
						currentSecondDecoder = nextSecondDecoder;
						nextSecondDecoder =
							new BMPDecoder(
								pi.videoContainer(),
								!currentSecondDecoder.isReversed(),
								getBuffer()[1].video(),
								this
							);
						playingPiece = getBuffer()[0];
						setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
					} else if (getBuffer().length > 0) {
						currentSecondDecoder = nextSecondDecoder;
						nextSecondDecoder = null;
						playingPiece = getBuffer()[0];
						setBuffer(Arrays.copyOfRange(getBuffer(), 1, getBuffer().length));
					} else {
						playingPiece = null;
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
