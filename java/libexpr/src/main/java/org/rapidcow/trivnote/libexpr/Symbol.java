/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interned {@link Atom}.
 * It is unquoted in wire S-expression representation.
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
	 * {@return the unique symbol for the given value}
	 * This function has the possible side-effect of
	 * creating a new entry in the symbol table.
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
	 */
	@Override
	public int
	compareTo(Symbol o)
	{
		return value.compareTo(o.value);
	}
}
