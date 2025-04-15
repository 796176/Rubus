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

package backend;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RequestValueChecker makes sure the values retrieved from rubus requests are valid. The request received from
 * the client is not guaranteed to be of the format the server expects due to the client malfunctioning, malicious
 * intents etc. RequestValueChecker is created to address this issue.
 */
public class RequestValueChecker {

	/**
	 * Checks if the id length is even and every character is hexadecimal.
	 * @param id the id
	 * @return the same id
	 * @throws IllegalArgumentException if the id is of a wrong format
	 */
	public String checkId(String id) {
		if (id.length() % 2 != 0) throw new IllegalArgumentException();
		Pattern pattern = Pattern.compile("^[a-f0-9]+$");
		Matcher matcher = pattern.matcher(id);
		if (!matcher.find()) throw new IllegalArgumentException();
		return id;
	}

	/**
	 * Checks if the value is negative.
	 * @param i the value
	 * @return the same value
	 * @throws IllegalArgumentException if the value is negative
	 */
	public int checkForNegative(int i) {
		if (i < 0) throw new IllegalArgumentException();
		return i;
	}

	/**
	 * Checks if the value is non-positive
	 * @param i the value
	 * @return the same value
	 * @throws IllegalArgumentException if the value is non-positive
	 */
	public int checkForNonPositive(int i) {
		if (i <= 0) throw new IllegalArgumentException();
		return i;
	}
}
