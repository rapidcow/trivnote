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
 */
public final
class
	Scalar
extends
	Atom
implements
	Comparable<Scalar>
{
	private boolean quot;

	/**
	 * Creates an immutable scalar atom.
	 *
	 * @param value  value
	 * @param isStr  whether this should be a quoted string
	 * @throws NullPointerException on null {@code value}
	 */
	private
	Scalar(String value, boolean isStr)
	{
		super(value);
		quot = isStr;
	}

	/**
	 * {@return whether this should be a quoted string}
	 * @since %v
	 */
	public final boolean
	isString()
	{
		return quot;
	}

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
		return new Scalar(value, false);
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
		return new Scalar(value, true);
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
	compareTo(Scalar o)
	{
		return value.compareTo(o.value);
	}
}
