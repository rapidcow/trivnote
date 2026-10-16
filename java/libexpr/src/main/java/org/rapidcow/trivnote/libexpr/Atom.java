/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * A non-cons non-nil {@link Datum}.
 * Concretely, it covers two types of atoms:
 *
 * <ul>
 *   <li> The {@link Scalar}, which holds an arbitrary value. </li>
 *   <li> The {@link Symbol}, which holds a value from a
 *        (hopefully) fixed pool of vocabulary. </li>
 * </ul>
 *
 * <p>
 * Presently, an atom’s value is a freeform Unicode
 * string, represented by {@link #value()}.
 */
public sealed
interface
	Atom
extends
	Datum
permits
	Scalar,
	Symbol
{
	/**
	 * {@return the value of this atom}
	 * The value may not be null.
	 */
	public abstract String value();
}
