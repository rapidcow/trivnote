/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

public final
class
	TruthScalar
extends
	Scalar
{
	public static final TruthScalar TRUE = new TruthScalar(true);
	public static final TruthScalar FALSE = new TruthScalar(false);

	private final boolean value;

	private
	TruthScalar(boolean value)
	{
		this.value = value;
	}

	@Override
	public final String
	value()
	{
		return value ? "#t" : "#f";
	}

	public final boolean
	truthValue()
	{
		return value;
	}

	public static TruthScalar
	of(boolean value)
	{
		return value ? TRUE : FALSE;
	}
}
