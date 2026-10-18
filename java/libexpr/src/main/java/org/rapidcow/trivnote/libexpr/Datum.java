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
	public static DatumList
	asList(Datum node)
	{
		return new DatumList(node).requireProperTail();
	}

	/**
	 * Return the car of given datum.
	 *
	 * @param node  datum
	 * @throws IllegalArgumentException if not a cons
	 * @since %v
	 */
	public static Datum
	car(Datum node)
	{
		if (node instanceof Cons cell) {
			return cell.car();
		}
		throw new IllegalArgumentException("not a cons");
	}

	/**
	 * Return the cdr of given datum.
	 *
	 * @param node  datum
	 * @throws IllegalArgumentException if not a cons
	 * @since %v
	 */
	public static Datum
	cdr(Datum node)
	{
		if (node instanceof Cons cell) {
			return cell.cdr();
		}
		throw new IllegalArgumentException("not a cons");
	}

	/**
	 * Return the value of given datum.
	 *
	 * @param node  datum
	 * @throws IllegalArgumentException not an atom
	 * @since %v
	 */
	public static String
	value(Datum node)
	{
		if (node instanceof Atom atom) {
			return atom.value();
		}
		throw new IllegalArgumentException("not an atom");
	}

	public static Datum
	fromList(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			return Nil.INSTANCE;
		}
		return Cons.fromList(list);
	}

	/**
	 * {@return the datum as an associative list}
	 *
	 * @param node  datum
	 * @throws IllegalArgumentException if the list
	 *    is not a proper list, or the list consists
	 *    of any non-cons datuk
	 * @since %v
	 */
	public static AssocList
	asAlist(Datum node)
	{
		return new AssocList(node);
	}
}
