/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * Character scalars, holding a single Unicode code point.
 * Represented as <q>{@code #\a}</q>, <q>{@code #\space}</q>,
 * <q>{@code #\x41}</q> and so on.
 *
 * @since %v
 */
public final
class
	CharScalar
implements
	Datum
{
	private final int codePoint;

	private
	CharScalar(int codePoint)
	{
		this.codePoint = codePoint;
	}

	/**
	 * {@return the character scalar for the given code point}
	 *
	 * @param codePoint  the code point
	 * @throws IllegalArgumentException if not a valid code point
	 */
	public static CharScalar
	of(int codePoint)
	{
		if (!Character.isValidCodePoint(codePoint)) {
			throw new IllegalArgumentException("invalid code point");
		}
		return new CharScalar(codePoint);
	}

	/**
	 * {@return the code point of this scalar}
	 */
	public final int
	codePointValue()
	{
		return codePoint;
	}

	@Override
	public boolean
	equals(Object o)
	{
		return o instanceof CharScalar c && c.codePoint == codePoint;
	}

	@Override
	public int
	hashCode()
	{
		return Integer.hashCode(codePoint);
	}

	/**
	 * {@return a string representation of this}
	 */
	@Override
	public String
	toString()
	{
		return String.format("Char[codePoint=U+%04X]", codePoint);
	}
}
