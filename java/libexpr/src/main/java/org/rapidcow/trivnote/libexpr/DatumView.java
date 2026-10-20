/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * View of a datum.  These classes compose on top of
 * a datum, but are otherwise not a datum themselves.
 * So they are not serializable; but you can make it
 * by having them return their underlying storage!
 */
public
interface
	DatumView
{
	/**
	 * {@return the underlying datum}
	 * The return value should always return
	 * the same object.
	 */
	Datum head();
}
