/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * Boolean scalars.  Booleans are singletons.
 *
 * @since 0.1.0
 */
public final
class
	BoolScalar
extends
	Scalar
{
	/**
	 * True.  Represented as <q>{@code #t}</q>.
	 */
	public static final BoolScalar TRUE = new BoolScalar(true);
	/**
	 * False.  Represented as <q>{@code #f}</q>.
	 */
	public static final BoolScalar FALSE = new BoolScalar(false);

	private final boolean value;

	private
	BoolScalar(boolean value)
	{
		this.value = value;
	}

	/**
	 * {@return a string representation of this}
	 */
	@Override
	public final String
	toString()
	{
		return value ? "TRUE" : "FALSE";
	}

	/**
	 * {@return the value of this atom}  This value is
	 * suitable for serialization.
	 */
	@Override
	public final String
	value()
	{
		return value ? "#t" : "#f";
	}

	/**
	 * {@return the boolean value of this scalar}
	 */
	public final boolean
	booleanValue()
	{
		return value;
	}

	/**
	 * {@return the boolean scalar for the given value}
	 *
	 * @param value  the given value
	 */
	public static BoolScalar
	of(boolean value)
	{
		return value ? TRUE : FALSE;
	}
}
