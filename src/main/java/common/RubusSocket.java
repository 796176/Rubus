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

package common;


public interface RubusSocket {

	void open();

	void open(long timeout);

	void close();

	void close(long timeout);

	int read(byte[] in);

	int read(byte[] in, long timeout);

	int read(byte[] in, int offset, int length);

	int read(byte[] in, int offset, int length, long timeout);

	int write(byte[] out);

	int write(byte[] out, long timeout);

	int write(byte[] out, int offset, int length);

	int write(byte[] out, int offset, int length, long timeout);

	boolean isClosed();

	long openedTime();

	long closedTime();

	long lastReceivedTime();

	long lastSentTime();
}
