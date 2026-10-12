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
public final class Scalar extends Atom
{
	/**
	 * Creates an immutable scalar atom.
	 *
	 * @param value  value
	 * @throws NullPointerException on null {@code value}
	 */
	public Scalar(String value)
	{
		super(value);
	}

	/**
	 * {@return whether this equals {@code o}}
	 * Two scalars are considered equal if their values equal.
	 *
	 * @param o  object to be compared against
	 */
	@Override public boolean equals(Object o)
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
	 * {@return hash code for this}
	 * Equal scalars hash to the same code.
	 */
	@Override public int hashCode()
	{
		return value.hashCode();
	}
}
