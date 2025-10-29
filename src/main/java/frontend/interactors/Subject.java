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

package frontend.interactors;

/**
 * The subject participant of the Observer pattern.
 */
public interface Subject {

	/**
	 * Adds a new observer.
	 * @param o a new observer
	 */
	void attach(Observer o);

	/**
	 * Removes an observer.
	 * @param o an observer
	 */
	void detach(Observer o);

	/**
	 * Returns all the observers of this Subject.
	 * @return all the observers of this Subject
	 */
	Observer[] getObservers();

	/**
	 * Sends a notification to all the observers that its state has changed.
	 */
	void sendNotification();
}
