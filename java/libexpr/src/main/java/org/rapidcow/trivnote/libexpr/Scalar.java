/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * A boolean, number, or string {@link Atom}.
 * The {@link #value()} of a boolean or number is
 * its wire representation.  A string’s value is
 * its unescaped content, without surrounding quotes.
 *
 * <p>
 * A {@code Scalar} is one of three values: boolean,
 * {@code #t} ({@link BoolScalar#TRUE}) and {@code #f}
 * ({@link BoolScalar#FALSE}); free-form strings (via
 * {@link Scalar#ofString(String)}); and numbers (via
 * {@link Scalar#ofNumber(String)}).
 *
 * <p>Scalars are compared by {@link #value} for equality
 * with the <em>sole exception</em> of strings, which can
 * be distinguished using {@link #isString()}.  The reason
 * is that while the value of a string scalar is an
 * unescaped string, the value of a non-string scalar
 * corresponds to its wire representation.  In other
 * words non-string scalars are the only datums who own
 * serialization logic outside of {@link DataDumper}.
 */
public sealed
abstract class
	Scalar
extends
	Atom
permits
	BoolScalar,
	NumberScalar,
	StringScalar
{
	/**
	 * Default constructor.
	 */
	protected Scalar()
	{
		/* nothing to do. */
	}

	/**
	 * {@return an immutable numeric scalar atom}
	 *
	 * <p>
	 * Usage:
	 * <pre>{@code Scalar v = Scalar.ofNumber("17/29");}</pre>
	 *
	 * If you need to access number-specific fields,
	 * declare your variable with the type
	 * {@link NumberScalar}.
	 *
	 * @param value  the given value
	 * @throws NullPointerException on null {@code value}
	 * @throws IllegalArgumentException on illegal number format
	 * @since 0.1.0
	 */
	public static NumberScalar
	ofNumber(String value)
	{
		return new SimpleScalar(value);
	}

	/**
	 * {@return an immutable string scalar atom}
	 *
	 * <p>
	 * Usage:
	 * <pre>{@code Scalar v = Scalar.ofString("abc");}</pre>
	 *
	 * @param value  the given value
	 * @throws NullPointerException on null {@code value}
	 * @since 0.1.0
	 */
	public static StringScalar
	ofString(String value)
	{
		return new StringScalar(value);
	}

	/**
	 * {@return whether this should be a quoted string}
	 *
	 * @since 0.1.0
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

	/**
	 * {@return hash code for this scalar}
	 * This implementation is consistent with
	 * {@link #equals(Object)}.
	 */
	@Override
	public final int
	hashCode()
	{
		return value().hashCode() ^ (isString() ? 1 : 0);
	}
}
