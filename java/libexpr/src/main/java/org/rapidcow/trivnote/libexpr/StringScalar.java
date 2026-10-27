/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * The string scalar type.  The only method
 * of use here is {@code value()}, which is
 * specified by {@link Atom#value()}.
 *
 * @since 0.1.0
 */
public non-sealed
abstract class
	StringScalar
implements
	Datum
{
	/**
	 * Default constructor.
	 */
	protected StringScalar()
	{
		/* nothing to do. */
	}

	/**
	 * {@return an immutable string scalar atom}
	 *
	 * <p>
	 * Usage:
	 * <pre>{@code Datum v = StringScalar.of("abc");}</pre>
	 *
	 * @param value  the given value
	 * @throws NullPointerException on null {@code value}
	 * @since 0.1.0
	 */
	public static StringScalar
	of(String value)
	{
		return new SimpleStringScalar(value);
	}

	/**
	 * {@return the value of this scalar}
	 * The return value may change across calls,
	 * but it should be equal under string equality.
	 */
	public abstract String
	value();

	/**
	 * {@return a good-enough string representation of this scalar}
	 */
	@Override
	public String
	toString()
	{
		return "String[value=" + value() + "]";
	}

	/**
	 * Compares this scalar against {@code o}.
	 * {@code o} is considered equal if and only if
	 * {@code o} is also a string scalar with an
	 * equal value.
	 *
	 * @param o  the object to compare against
	 * @return whether {@code o} is an equal scalar
	 */
	@Override
	public final boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}
		if (o instanceof StringScalar fren) {
			return value().equals(fren.value());
		}
		return false;
	}

	/**
	 * {@return hash code for this scalar}
	 * This implementation is consistent with
	 * {@link #equals(Object)}.
	 */
	@Override
	public final int
	hashCode()
	{
		return value().hashCode();
	}
}
