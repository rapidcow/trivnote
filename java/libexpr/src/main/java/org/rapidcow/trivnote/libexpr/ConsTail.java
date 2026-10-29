/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

/**
 * Cons cell with a nil cdr.
 *
 * @since 0.1.0
 */
public final
class
	ConsTail
extends
	Cons
{
	private final Datum car;

	/**
	 * Creates a new car-only cons cell.
	 *
	 * @param car  the car
	 * @throws NullPointerException if car is null
	 */
	public
	ConsTail(Datum car)
	{
		this.car = Objects.requireNonNull(car);
	}

	/**
	 * {@return the car}
	 */
	@Override
	public final Datum
	car()
	{
		return car;
	}

	/**
	 * {@return nil}
	 */
	@Override
	public final Datum
	cdr()
	{
		return Nil.INSTANCE;
	}
}
