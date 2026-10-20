/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/*
 * Note: these imports are for javadoc
 */
import java.util.List;
import java.util.Map;

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
	 * This method checks for more than {@link Atom}
	 * class: namely, nil is an atom devoid of value.
	 *
	 * @param node  the datum
	 */
	public static boolean
	isAtom(Datum node)
	{
		return !(node instanceof Cons);
	}

	/**
	 * {@return whether datum is a list}
	 * A list is either a cons cell or nil.
	 * An atom is <em>not</em> considered a list, despite
	 * {@link DatumList} treating it as an empty list with
	 * an improper terminator.
	 *
	 * <p>
	 * This is a basic type check and does not check that
	 * the terminator is nil by the way.  To validate a
	 * <em>proper</em> list, use {@link DatumList#isProperList}.
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
	 * This value conforms to the interface
	 * <code>{@linkplain List List}&lt;{@linkplain Datum Datum},{@linkplain Datum Datum}&gt;</code>.
	 *
	 * @param node  the datum
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
	 * {@return the car of the given datum}
	 *
	 * @param node  the given datum
	 * @throws IllegalArgumentException if not a cons
	 * @since %v
	 * @see Cons#car()
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
	 * {@return the cdr of the given datum}
	 *
	 * @param node  the given datum
	 * @throws IllegalArgumentException if not a cons
	 * @since %v
	 * @see Cons#cdr()
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
	 * {@return the value of the given datum}
	 *
	 * @param node  the given datum
	 * @throws IllegalArgumentException not an atom
	 * @since %v
	 * @see Atom#value()
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
	 * This value conforms to the interface
	 * <code>{@linkplain Map Map}&lt;{@linkplain Datum Datum},{@linkplain Datum Datum}&gt;</code>.
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

	public static AssocList
	fromMap(Map<? extends Datum, ? extends Datum> map)
	{
		return new AssocList(map);
	}
}
