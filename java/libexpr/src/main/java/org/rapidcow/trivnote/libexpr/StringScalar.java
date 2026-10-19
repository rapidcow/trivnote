/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

public final
class
	StringScalar
extends
	Scalar
{
	private final String value;

	/*
	 * Do not depend on this.  Use Scalar.ofString()
	 */
	public
	StringScalar(String value)
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
		return "String[value=" + value + "]";
	}
}
