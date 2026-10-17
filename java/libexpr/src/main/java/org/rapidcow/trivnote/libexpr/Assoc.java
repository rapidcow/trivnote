/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

public final
class
	Assoc
extends
	Cons
{
	private final Symbol car;
	private final Datum cdr;

	public
	Assoc(Symbol key, Datum value)
	{
		car = Objects.requireNonNull(key, "key");
		cdr = Objects.requireNonNull(value, "value");
	}

	@Override
	public final Symbol
	car()
	{
		return car;
	}

	@Override
	public final Datum
	cdr()
	{
		return cdr;
	}
}
