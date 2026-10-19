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
	Scalar
{
	private final String value;

	public
	SimpleScalar(String value)
	{
		this.value = Objects.requireNonNull(value);
	}

	@Override
	public final String
	value()
	{
		return value;
	}

	@Override
	public String
	toString()
	{
		return "Scalar[value=" + value + "]";
	}
}
