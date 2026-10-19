/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

final
class
	SimpleScalar
extends
	NumberScalar
{
	private final NumberParser result;

	public
	SimpleScalar(String value)
	{
		Objects.requireNonNull(value);
		result = new NumberParser(value);
	}

	@Override
	public final String
	value()
	{
		return prefix() + sign() + mantissa() + exponent();
	}

	@Override
	public String
	toString()
	{
		return "Scalar[value=" + value() + "]";
	}

	@Override
	public String
	prefix()
	{
		return result.getPrefix();
	}

	@Override
	public Exactness
	exactness()
	{
		return result.getExactness();
	}

	@Override
	public int
	radix()
	{
		return result.getRadix();
	}

	@Override
	public String
	number()
	{
		return sign() + mantissa() + exponent();
	}

	@Override
	public String
	sign()
	{
		return result.getSign();
	}

	@Override
	public String
	mantissa()
	{
		return result.getMantissa();
	}

	@Override
	public String
	exponent()
	{
		return result.getExponent();
	}
}
