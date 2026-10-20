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
 * Note that {@link Nil} is both an <q>{@linkplain #isAtom(Datum) atom}</q>
 * (to be distinguished from the {@link Atom} class, which is
 * exclusively atoms with a {@linkplain Atom#value() value}) and a
 * <q>{@linkplain #isList(Datum) list}</q>.
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
	 * A non-nil atom is <em>not</em> considered a list,
	 * despite {@link DatumList} treating it as an empty
	 * list with an improper terminator.
	 *
	 * <p>
	 * This is a basic type check and does not check that
	 * the terminator is nil.  To validate a
	 * <em>proper</em> list, use {@link DatumList#isProperList()}.
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
	 * The returned view implements {@code List<Datum>} and
	 * retains the source datum without copying its elements.
	 * Nil produces an empty view.
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
	 * @throws IllegalArgumentException if {@code node} is not an
	 *    {@link Atom} instance; nil has no value
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

	/**
	 * {@return a proper list containing the given elements in order}
	 * An empty source produces {@link Nil#INSTANCE}.  Otherwise,
	 * the elements are copied into a cons list; later changes to
	 * the source list do not affect the returned datum.
	 *
	 * @param list  the source elements
	 * @throws NullPointerException if the list or any element is null
	 * @since %v
	 * @see Cons#fromList(List)
	 */
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
	 * The returned view implements {@code Map<Datum, Datum>}.
	 * Each element must be a cons whose car is the key and whose
	 * cdr is the value.  If keys compare equal, the first entry
	 * supplies the value.  Nil produces an empty view.
	 *
	 * @param node  datum
	 * @throws IllegalArgumentException if the list
	 *    is not a proper list, or the list consists
	 *    of any non-cons datum
	 * @throws NullPointerException if {@code node} is null
	 * @since %v
	 */
	public static AssocList
	asAlist(Datum node)
	{
		return new AssocList(node);
	}

	/**
	 * {@return an associative list view of a copy of the map entries}
	 * Entries become cons pairs in the iteration order of the
	 * source map's entry set.  The underlying list is proper and
	 * is nil for an empty map.  Later changes to the source map
	 * do not affect the returned view.
	 *
	 * @param map  the source key-value mappings
	 * @throws NullPointerException if the map, any key, or any value is null
	 * @since %v
	 * @see AssocList#head()
	 */
	public static AssocList
	fromMap(Map<? extends Datum, ? extends Datum> map)
	{
		return new AssocList(map);
	}
}
