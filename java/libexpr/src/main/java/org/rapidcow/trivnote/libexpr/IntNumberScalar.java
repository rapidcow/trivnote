/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.OptionalInt;

/**
 * A decimal, unprefixed integer backed by an {@code int}.
 */
final
class
	IntNumberScalar
extends
	NumberScalar
{
	private final int n;

	IntNumberScalar(int n)
	{
		this.n = n;
	}

	/**
	 * {@return the primitive value}
	 */
	int
	intValue()
	{
		return n;
	}

	@Override
	public String
	value()
	{
		return Integer.toString(n);
	}

	@Override
	public String
	prefix()
	{
		return "";
	}

	@Override
	public Exactness
	exactness()
	{
		return Exactness.UNSPECIFIED;
	}

	@Override
	public int
	radix()
	{
		return 10;
	}

	@Override
	public String
	number()
	{
		return value();
	}

	@Override
	public String
	sign()
	{
		return (n < 0) ? "-" : "";
	}

	@Override
	public String
	mantissa()
	{
		/* Integer.MIN_VALUE has no positive counterpart */
		return (n < 0) ? value().substring(1) : value();
	}

	@Override
	public String
	exponent()
	{
		return "";
	}

	@Override
	public boolean
	isNaN()
	{
		return false;
	}

	@Override
	public boolean
	isInfinite()
	{
		return false;
	}

	@Override
	public OptionalInt
	tryCompareNumericTo(NumberScalar o)
	{
		if (o instanceof IntNumberScalar that) {
			return OptionalInt.of(Integer.compare(n, that.n));
		}
		return OptionalInt.empty();
	}
}
