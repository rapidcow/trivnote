/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * View of a datum.  These classes compose on top of
 * a datum, but are otherwise not a datum themselves.
 * So they are not serializable.  Conversely though,
 * all datum views are fully represented by the datum
 * it encloses, which you <em>can</em> serialize.
 * This can be useful when the view was itself created
 * from scratch: taking the {@link #head()} gives you
 * a complete, serializable form to work with.
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
