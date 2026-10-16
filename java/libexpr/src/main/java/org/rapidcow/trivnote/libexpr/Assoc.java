/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

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
		car = key;
		cdr = value;
	}

	@Override
	public Symbol
	car()
	{
		return car;
	}

	@Override
	public Datum
	cdr()
	{
		return cdr;
	}
}
