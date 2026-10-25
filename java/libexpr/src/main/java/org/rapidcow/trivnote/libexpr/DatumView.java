/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * View of a datum.  A view exposes its underlying datum through
 * {@link #head()}, allowing it to be used for serialization or traversal.
 * A {@link Datum} is its own view; other implementations can compose
 * domain-specific behavior on top of a datum.
 *
 * @since 0.1.0
 */
public
interface
	DatumView
{
	/**
	 * {@return the underlying source datum}
	 * The return value should always return
	 * the same object.
	 */
	Datum head();
}
