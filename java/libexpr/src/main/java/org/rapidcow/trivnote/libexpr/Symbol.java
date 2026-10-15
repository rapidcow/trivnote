/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Special {@link Atom} that tends to repeat itself in many
 * places.  It is unquoted in wire S-expression representation.
 */
public final
class
	Symbol
extends
	Atom
implements
	Comparable<Symbol>
{
	private
	Symbol(String value)
	{
		super(value);
	}

	/**
	 * {@return a new symbol for the given value}
	 *
	 * @param value  the given value
	 * @throws NullPointerException on null {@code value}
	 */
	public static Symbol
	of(String value)
	{
		return new Symbol(value);
	}

	/**
	 * Return the comparison result of this against {@code o}.
	 * Scalars do not equal symbols or any other subtype of
	 * {@link Atom}, even if their values agree.
	 *
	 * @param o  symbol to compare
	 * @return the comparison result
	 * @since %v
	 */
	@Override
	public int
	compareTo(Symbol o)
	{
		return value.compareTo(o.value);
	}
}
