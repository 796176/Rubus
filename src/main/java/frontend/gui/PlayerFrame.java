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

import frontend.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.function.Consumer;

public class PlayerFrame extends JFrame implements PlayerInterface, Subject {

	private Consumer<Graphics> consumer = null;

	private final ArrayList<Observer> observers = new ArrayList<>();

	private long currentPieceIndex;

	private final long totalPieces;

	private final double pieceDuration;

	private PlaybackPiece[] playbackBuffer = new PlaybackPiece[0];

	public PlayerFrame(long initialPieceIndex, long totalPieces, double pieceDuration) {
		assert initialPieceIndex >= 0 && totalPieces > 0 && pieceDuration > 0;

		this.totalPieces = totalPieces;
		this.pieceDuration = pieceDuration;
		setPlayingPiece(initialPieceIndex);

		setSize(500, 500);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				consumer = g -> {
					g.drawString("1   " + System.currentTimeMillis(), 90, 90);
					sendNotification();
				};
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent mouseEvent) {

			}

			@Override
			public void mouseReleased(MouseEvent mouseEvent) {

			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent) {

			}

			@Override
			public void mouseExited(MouseEvent mouseEvent) {

			}
		});
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (consumer != null) consumer.accept(g);
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public boolean isPaused() {
		return false;
	}

	@Override
	public long getPlayingPiece() {
		return currentPieceIndex;
	}

	@Override
	public long getTotalPieces() {
		return totalPieces;
	}

	@Override
	public double getPieceDuration() {
		return pieceDuration;
	}

	@Override
	public void setPlayingPiece(long timestamp) {
		assert timestamp < totalPieces;

		currentPieceIndex = timestamp;
		sendNotification();
	}

	@Override
	public PlaybackPiece[] getBuffer() {
		return playbackBuffer;
	}

	@Override
	public void setBuffer(PlaybackPiece[] buffer) {
		assert buffer != null;

		playbackBuffer = buffer;
		sendNotification();
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
}
