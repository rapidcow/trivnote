/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * The atom devoid of value and the empty list.
 */
public
enum
	Nil
implements
	Datum
{
	/** The singleton that represents {@code nil}. */
	INSTANCE;

	/**
	 * {@return this}
	 */
	@Override
	public final Nil
	head()
	{
		return this;
	}
}
