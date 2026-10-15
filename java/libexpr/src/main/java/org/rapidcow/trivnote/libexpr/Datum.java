/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.List;

/**
 * Basic unit of an S-expression.
 * A datum is either an {@link Atom}, a {@link Cons},
 * or {@link Nil}.
 *
 * <p>
 * Note that {@link Nil} is both an “atom” (to be
 * distinguished from the {@link Atom} class) and
 * a “list”.
 */
public sealed
interface
	Datum
permits
	Atom,
	Cons,
	Nil
{
	/**
	 * {@return whether the datum is an atom}
	 *
	 * @param node  the datum
	 */
	public static boolean
	isAtom(Datum node)
	{
		return !(node instanceof Cons);
	}

	/**
	 * {@return whether datum is a list}  A list
	 * is either a cons cell or nil.  A list is
	 * iterable, though you must cast it to the
	 * specific class to please the compiler.
	 *
	 * @param node  the datum
	 */
	public static boolean
	isList(Datum node)
	{
		return !(node instanceof Atom);
	}

	/**
	 * {@return the datum as a proper list}
	 *
	 * @param node  datum
	 * @throws IllegalArgumentException if the list
	 *    terminator is not nil; i.e. improper list
	 * @since %v
	 */
	public static List<Datum>
	asList(Datum node)
	{
		return new DatumList(node).requireProperTail();
	}

	/**
	 * @since %v
	 */
	public static Datum
	car(Datum node)
	{
		return ((Cons) node).car();
	}

	/**
	 * @since %v
	 */
	public static Datum
	cdr(Datum node)
	{
		return ((Cons) node).cdr();
	}

	/**
	 * @since %v
	 */
	public static String
	value(Datum node)
	{
		return ((Atom) node).value();
	}
}
