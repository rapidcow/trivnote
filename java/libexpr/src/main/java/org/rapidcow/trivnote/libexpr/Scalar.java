/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * Regular {@link Atom} meant to represent a diverse
 * range of values.  A scalar is represented by its
 * string representation stored in {@link #value()}.
 *
 * @see SimpleScalar
 * @see StringScalar
 */
public non-sealed
abstract class
	Scalar
implements
	Atom,
	Comparable<Scalar>
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

	@Override
	public boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}
		if (o instanceof Scalar atom) {
			return value().equals(atom.value());
		}
		return false;
	}

	@Override
	public int
	hashCode()
	{
		return value().hashCode();
	}

	@Override
	public int
	compareTo(Scalar o)
	{
		return value().compareTo(o.value());
	}
}
