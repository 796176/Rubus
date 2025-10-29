/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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

package frontend.stubs;

import frontend.exceptions.NotImplementedExceptions;
import frontend.network.RubusRequest;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

public class RubusRequestBuilderStub implements RubusRequest.Builder {

	@FunctionalInterface
	public interface TriConsumer<T1, T2, T3> {
		void accept(T1 t1, T2 t2, T3 t3);
	}

	public RubusRequest rubusRequest = new RubusRequest() { };

	public Consumer<String> hostConsumer = h -> { throw new NotImplementedExceptions(); };

	public Consumer<Integer> portConsumer = p -> { throw new NotImplementedExceptions(); };

	public Runnable listRunnable = () -> { throw new NotImplementedExceptions(); };

	public Consumer<String> listConsumer = q -> { throw new NotImplementedExceptions(); };

	public Consumer<String> infoConsumer = id -> { throw new NotImplementedExceptions(); };

	public TriConsumer<String, Integer, Integer> fetchConsumer = (id, i1, i2) -> {
		throw new NotImplementedExceptions();
	};

	@Override
	public RubusRequest.Builder host(@Nonnull String host) {
		hostConsumer.accept(host);
		return this;
	}

	@Override
	public RubusRequest.Builder port(int port) {
		portConsumer.accept(port);
		return this;
	}

	@Override
	public RubusRequest.Builder LIST() {
		listRunnable.run();
		return this;
	}

	@Override
	public RubusRequest.Builder LIST(@Nonnull String searchQuery) {
		listConsumer.accept(searchQuery);
		return this;
	}

	@Override
	public RubusRequest.Builder INFO(@Nonnull String mediaId) {
		infoConsumer.accept(mediaId);
		return this;
	}

	@Override
	public RubusRequest.Builder FETCH(@Nonnull String mediaID, int offset, int amount) {
		fetchConsumer.accept(mediaID, offset, amount);
		return this;
	}

	@Override
	public RubusRequest build() {
		return rubusRequest;
	}
}
