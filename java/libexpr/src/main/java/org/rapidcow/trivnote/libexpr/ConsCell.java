/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

/**
 * Simple cons cells.  They are simple in that they do
 * not know their length or their terminator.
 * This was the original implementation of {@link Cons}
 * before it abstracted.
 *
 * @since %v
 */
public final
class
	ConsCell
extends
	Cons
{
	private final Datum car;
	private final Datum cdr;

	/**
	 * Creates an immutable cons cell.
	 *
	 * @param car  the former half
	 * @param cdr  the latter half
	 * @throws NullPointerException on null {@code car} or {@code cdr}
	 */
	public
	ConsCell(Datum car, Datum cdr)
	{
		this.car = Objects.requireNonNull(car, "car");
		this.cdr = Objects.requireNonNull(cdr, "cdr");
	}

	/**
	 * {@return the former half}
	 */
	public final Datum
	car()
	{
		return car;
	}

	/**
	 * {@return the latter half}
	 */
	public final Datum
	cdr()
	{
		return cdr;
	}
}
