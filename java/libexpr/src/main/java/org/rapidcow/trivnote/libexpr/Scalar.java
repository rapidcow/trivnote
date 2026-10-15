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
	 */
	public static Scalar
	ofString(String value)
	{
		return new Scalar(value, true);
	}

	/**
	 * Returns whether this equals {@code o}.
	 * Two scalars are considered equal if their values equal.
	 * Scalars do not equal symbols or any other subtype of
	 * {@link Atom}, even if their values agree.
	 *
	 * @param o  object to compare
	 * @return whether this equals {@code o}
	 */
	@Override
	public boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}
		if (o instanceof Scalar atom) {
			return value.equals(atom.value);
		}
		return false;
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
	compareTo(Scalar o)
	{
		return value.compareTo(o.value);
	}
}
