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

package frontend;

import java.util.ArrayList;

/**
 * A subject participant of the Observer pattern.
 */
public interface Subject {

	/**
	 * Adds an observer to this subject.
	 * @param o a new observer
	 */
	void attach(Observer o);

	/**
	 * Removes an observer from this subject
	 * @param o an observer
	 */
	void detach(Observer o);

	/**
	 * Returns an array of all the observers.
	 * @return an array of observers
	 */
	Observer[] getObservers();

	/**
	 * Notifies its subject about its state change.
	 */
	void sendNotification();
}
