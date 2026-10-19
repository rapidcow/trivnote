/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

public
enum
	Bool
implements
	Atom
{
	TRUE(true),
	FALSE(false);

	private final boolean value;

	private
	Bool(boolean value)
	{
		this.value = value;
	}

	@Override
	public final String
	value()
	{
		return value ? "#t" : "#f";
	}

	public static Bool
	of(boolean value)
	{
		return value ? TRUE : FALSE;
	}
}
