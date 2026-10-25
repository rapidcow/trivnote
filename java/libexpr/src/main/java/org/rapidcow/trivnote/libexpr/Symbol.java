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
implements
	Atom,
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
	 * @since 0.1.0
	 */
	public
	Symbol(String value)
	{
		this.value = Objects.requireNonNull(value);
	}

	/**
	 * {@return this}
	 */
	@Override
	public final Symbol
	head()
	{
		return this;
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

	@Override
	public int
	hashCode()
	{
		return value().hashCode();
	}

	/**
	 * Return the comparison result of this against {@code o}.
	 * Scalars do not equal symbols, even if their values agree.
	 *
	 * @param o  symbol to compare
	 * @return the comparison result
	 * @since 0.1.0
	 */
	@Override
	public int
	compareTo(Symbol o)
	{
		return value.compareTo(o.value);
	}
}
