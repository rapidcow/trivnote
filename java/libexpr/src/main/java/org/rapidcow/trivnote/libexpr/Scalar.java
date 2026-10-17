/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * Regular {@link Atom} meant to represent a diverse
 * range of values.  A scalar is represented by its
 * string representation specified by {@link #value()}.
 *
 * @see SimpleScalar
 * @see StringScalar
 */
public non-sealed
abstract class
	Scalar
implements
	Atom
{
	/**
	 * {@return an immutable scalar atom}
	 *
	 * @param value  the given value
	 * @throws NullPointerException on null {@code value}
	 * @since %v
	 */
	public static Scalar
	of(String value)
	{
		return new SimpleScalar(value);
	}

	/**
	 * {@return an immutable scalar atom}
	 * This variant of {@link Scalar#of} hints to
	 * {@link DataDumper} that it must always be quoted.
	 *
	 * @param value  the given value
	 * @throws NullPointerException on null {@code value}
	 * @since %v
	 */
	public static Scalar
	ofString(String value)
	{
		return new StringScalar(value);
	}

	/**
	 * {@return whether this should be a quoted string}
	 * @since %v
	 */
	public final boolean
	isString()
	{
		return this instanceof StringScalar;
	}

	/**
	 * Compares this scalar against {@code o} for equality.
	 * Two scalars are equal if and only if they agree on
	 * both {@link #isString()} and {@link #value()}; the
	 * concrete class is otherwise irrelevant.
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
		if (o instanceof Scalar atom) {
			return isString() == atom.isString()
				&& value().equals(atom.value());
		}
		return false;
	}

	@Override
	public final int
	hashCode()
	{
		return value().hashCode() ^ (isString() ? 1 : 0);
	}
}
