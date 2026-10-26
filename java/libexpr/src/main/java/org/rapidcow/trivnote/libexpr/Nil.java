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
		/*
		 * Collections.emptyList().hashCode() is 1.
		 *
		 * Our Cons is based on that, and nil is the
		 * empty list; so it seems fair to do this :]
		 */
		return 1;
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
