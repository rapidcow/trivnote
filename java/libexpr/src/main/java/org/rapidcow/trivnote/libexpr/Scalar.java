/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * Non-interned {@link Atom}.  A scalar is
 * represented by its string representation
 * stored in {@link #value()}.
 */
public final
class
	Scalar
extends
	Atom
implements
	Comparable<Scalar>
{
	/**
	 * Creates an immutable scalar atom.
	 *
	 * @param value  value
	 * @throws NullPointerException on null {@code value}
	 */
	public
	Scalar(String value)
	{
		super(value);
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
