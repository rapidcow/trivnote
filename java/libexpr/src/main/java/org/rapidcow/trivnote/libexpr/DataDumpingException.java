/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * A datum exceeds a configured serialization limit.
 */
public final
class
	DataDumpingException
extends
	RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a dumping exception.
	 * @param message description of the exceeded limit
	 */
	public
	DataDumpingException(String message)
	{
		super(message);
	}
}
