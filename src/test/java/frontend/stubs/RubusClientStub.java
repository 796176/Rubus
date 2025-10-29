/*
 * Rubus is an application layer protocol for video and audio streaming and
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

package frontend.stubs;

import frontend.exceptions.NotImplementedExceptions;
import frontend.network.RubusClient;
import frontend.network.RubusRequest;
import frontend.network.RubusResponse;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

public class RubusClientStub implements RubusClient {

	@FunctionalInterface
	public interface SendFunction<T, U, R> {
		R apply(T t, U u) throws InterruptedException;
	}

	public SendFunction<RubusRequest, Long, RubusResponse> sendFunction = (client, l) -> {
		throw new NotImplementedExceptions();
	};

	public Supplier<RubusRequest.Builder> getRequestBuilderSupplier = () -> { throw new NotImplementedExceptions(); };

	public Runnable closeRunnable = () -> { };

	@Override
	public RubusResponse send(@Nonnull RubusRequest request, long timeout) throws InterruptedException {
		return sendFunction.apply(request, timeout);
	}

	@Override
	public RubusRequest.Builder getRequestBuilder() {
		return getRequestBuilderSupplier.get();
	}

	@Override
	public void close() {
		closeRunnable.run();
	}
}
