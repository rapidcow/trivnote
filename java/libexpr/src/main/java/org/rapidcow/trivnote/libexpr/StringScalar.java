/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

/**
 * The string scalar type.  The only method
 * of use here is {@code value()}, which is
 * specified by {@link Atom#value()}.
 *
 * @since 0.1.0
 */
public non-sealed
abstract class
	StringScalar
implements
	Datum
{
	/**
	 * {@return the value of this atom}
	 * The return value may change across calls,
	 * but it should be equal under string equality.
	 */
	public abstract String
	value()
	{
		return value;
	}

	/**
	 * {@return a good-enough string representation of this scalar}
	 */
	@Override
	public String
	toString()
	{
		return "String[value=" + value + "]";
	}
}
