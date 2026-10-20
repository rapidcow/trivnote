/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * A non-nil terminal {@link Datum}.
 * Concretely, it covers two types of atoms:
 *
 * <ul>
 *   <li> The {@link Scalar}, which holds a quoted string,
 *        a number, or a boolean. </li>
 *   <li> The {@link Symbol}, which holds unquoted strings,
 *        usually from a fixed pool of keyword vocabulary. </li>
 * </ul>
 *
 * <p>
 * An atom’s value is a freeform Unicode string,
 * represented by {@link #value()}.
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
	 *
	 * <p>
	 * The return value may change across calls,
	 * but it should be equal under string equality.
	 */
	String value();
}
