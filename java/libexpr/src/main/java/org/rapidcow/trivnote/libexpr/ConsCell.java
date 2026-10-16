/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;
import java.util.Objects;

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
