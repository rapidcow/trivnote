/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interned {@link Atom}.
 * It is unquoted in wire S-expression representation.
 */
public final class Symbol extends Atom
{
	/**
	 * Global symbol pool.
	 */
	private static final Map<String, Symbol> pool
		= new ConcurrentHashMap<>();

	private Symbol(String value)
	{
		super(value);
	}

	/**
	 * {@return the unique symbol for the given value}
	 * This function has the possible side-effect of
	 * creating a new entry in the symbol table.
	 *
	 * @param value  the given value
	 * @throws NullPointerException on null {@code value}
	 */
	public static Symbol of(String value)
	{
		/*
		 * Even if Symbol::new (which calls Atom::new) already
		 * checks for null, HashMap would be totally uncool with
		 * looking up a null key, so we must catch it early here.
		 */
		if (value == null) {
			throw new NullPointerException("value of a Symbol must not be null");
		}
		return pool.computeIfAbsent(value, Symbol::new);
	}
}
