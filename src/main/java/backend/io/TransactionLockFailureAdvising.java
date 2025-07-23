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

package backend.io;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.dao.ConcurrencyFailureException;

/**
 * An aspect to address concurrency transaction failures in DAOs.<br>
 * Modern databases use the MVCC mechanism as the opposite to explicit locking to provide data consistency when
 * multiple transactions run simultaneously. As an example, in Postgres it's achieved by simply running all transactions
 * on the Serializable isolation level. But this in turn causes transactions to fail more often.
 * TransactionLockFailureAdvising solves it by retrying failed transactions.<br><br>
 *
 * For more information on the data consistency see
 * <a href="https://www.postgresql.org/docs/current/applevel-consistency.html">postgresql.org/docs/current/applevel-consistency.html</a>
 */
@Aspect
public class TransactionLockFailureAdvising {

	private int retryAttempts;

	/**
	 * Constructs this aspect.
	 * @param retryAttempts specifies how many times a failed ( only due to serialization failures ) transaction can be
	 *                      retried
	 */
	public TransactionLockFailureAdvising(int retryAttempts) {
		setLockFailureRetryAttempts(retryAttempts);
	}

	@Pointcut("execution(* *getMedia*(..)) || execution(* *availableMedia*(..)) || execution(* *searchMedia*(..))")
	private void anyTransactionalMethod() {}

	/**
	 * An around type advice taken upon any joint point of database access methods of {@link MediaPool}.
	 * @param jp proceeding joint point
	 * @return the same value returned by the joint point
	 * @throws Throwable any Throwable thrown by the joint point including {@link ConcurrencyFailureException} if
	 *                   the transaction has failed more than retryAttempts times.
	 */
	@Around("within(backend.io.MediaPool) && anyTransactionalMethod()")
	public Object transactionExecution(ProceedingJoinPoint jp) throws Throwable {
		int attemptsLeft = retryAttempts;
		while (true) {
			try {
				return jp.proceed();
			} catch (ConcurrencyFailureException e) {
				if (attemptsLeft == 0) throw e;
				attemptsLeft--;
			}
		}
	}

	/**
	 * Returns how many times a failed transaction can be retried.
	 * @return how many times a failed transaction can be retried
	 */
	public int getLockFailureRetryAttempts() {
		return retryAttempts;
	}

	/**
	 * Sets how many times a failed transaction can be retried.
	 * @param retryAttempts how many times a failed transaction can be retried
	 */
	public void setLockFailureRetryAttempts(int retryAttempts) {
		assert retryAttempts >= 0;

		this.retryAttempts = retryAttempts;
	}
}
