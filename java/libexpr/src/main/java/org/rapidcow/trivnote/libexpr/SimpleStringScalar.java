/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

/**
 * A simple string scalar is only backed by a native Java string.
 */
final
class
	SimpleStringScalar
extends
	StringScalar
{
	private final String value;

	/**
	 * Constructs a new string scalar.
	 *
	 * @param value  the given value
	 */
	public
	SimpleStringScalar(String value)
	{
		this.value = Objects.requireNonNull(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String
	value()
	{
		return value;
	}
}
