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
 */
public final
class
	StringScalar
extends
	Scalar
{
	private final String value;

	/**
	 * Constructs a new string scalar.
	 * <b>Do not depend on this.</b>  Use {@link
	 * Scalar#ofString(String)}.
	 *
	 * @param value  the given value
	 */
	public
	StringScalar(String value)
	{
		this.value = Objects.requireNonNull(value);
	}

	/**
	 * Returns the value of this atom.
	 * The value is always non-null.
	 *
	 * @return {@inheritDoc}
	 */
	@Override
	public final String
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
