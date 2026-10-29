/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

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
	private final String value;

	/**
	 * Constructs a new symbol.  You may use this now
	 * that the global symbol cache is gone, but it is
	 * better to still use {@link Symbol#of(String)}
	 * in case the underlying implementation changes.
	 *
	 * @param value  symbol value
	 * @throws NullPointerException on null {@code value}
	 * @since 0.1.0
	 */
	public
	Symbol(String value)
	{
		this.value = Objects.requireNonNull(value);
	}

	/**
	 * {@return the value of this symbol}
	 */
	@Override
	public final String
	value()
	{
		return value;
	}

	/**
	 * {@return a diagnostic string containing this symbol’s value}
	 */
	@Override
	public String
	toString()
	{
		return "Symbol[value=" + value + "]";
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
	 * {@inheritDoc}
	 */
	@Override
	public boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}
		if (o instanceof Symbol name) {
			return value().equals(name.value());
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int
	hashCode()
	{
		return value().hashCode();
	}

	/**
	 * Compares symbol values lexicographically.
	 * This ordering is consistent with symbol equality.
	 *
	 * @param o  symbol to compare
	 * @return a negative integer, zero, or a positive
	 *    integer as this value sorts before, equals,
	 *    or sorts after the other value
	 * @throws NullPointerException on null {@code o}
	 * @since 0.1.0
	 */
	@Override
	public int
	compareTo(Symbol o)
	{
		return value.compareTo(o.value);
	}
}
