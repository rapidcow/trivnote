/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * The atom devoid of value and the empty list.
 */
public final
class
	Nil
extends
	Datum
{
	/** The singleton that represents {@code nil}. */
	public static final Nil INSTANCE = new Nil();

	private
	Nil()
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean
	equals(Object o)
	{
		return this == o;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int
	hashCode()
	{
		return System.identityHashCode(this);
	}

	/**
	 * {@return a string representation of nil}
	 */
	@Override
	public final String
	toString()
	{
		return "()";
	}
}
