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
 *
 * <p>
 * {@code Datum} promises no interface methods explicitly,
 * though through being a sealed interface it limits the
 * classes that can implement it.  For that reason we can
 * guarantee that all datums implement
 * <code>{@linkplain Object#hashCode() hashCode()}</code> and
 * <code>{@linkplain Object#equals(Object) equals(Object)}</code>
 * in a way such that they are compared by value, not by
 * object identity.
 */
public sealed
interface
	Datum
extends
	DatumView
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
	 * {@return whether the datum is a list}
	 * A list is either a cons cell or nil.
	 * A non-nil atom is <em>not</em> considered a list,
	 * despite {@link DatumList} treating it as an empty
	 * list with itself as the improper terminator.
	 *
	 * <p>
	 * This is a basic type check and does not check that
	 * the terminator is nil.  To validate a <em>proper</em>
	 * list, use {@link #isProperList(Datum)} or {@link
	 * #asList(Datum)} if you need to access its elements.
	 *
	 * @param node  the datum
	 */
	public static boolean
	isList(Datum node)
	{
		return !(node instanceof Atom);
	}

	/**
	 * {@return whether datum is a proper list}
	 * This is a stronger version of {@link #isList(Datum)}
	 * that requires a nil terminator.
	 *
	 * @param node  the datum
	 * @since 0.1.0
	 */
	public static boolean
	isProperList(Datum node)
	{
		return new DatumList(node).isProperList();
	}

	/**
	 * Returns the datum as a list.
	 * The returned view conforms to the interface
	 * <code>{@linkplain List List}&lt;{@linkplain Datum Datum},{@linkplain Datum Datum}&gt;</code>.
	 *
	 * <p>
	 * Note this view is even more permissive than
	 * {@link #isAtom}, treating non-nil atoms as empty
	 * lists with itself as the improper terminator.
	 * If this seems unacceptable, chain this call with
	 * <q><code>.{@linkplain DatumList#requireProperTail() requireProperTail()}</code></q>
	 * or use {@link #asProperList(Datum)}.
	 *
	 * @param node  the datum
	 * @return a view as a list
	 * @since 0.1.0
	 */
	public static DatumList
	asList(Datum node)
	{
		return new DatumList(node);
	}

	/**
	 * Returns the datum as a proper list.
	 * The returned view conforms to the interface
	 * <code>{@linkplain List List}&lt;{@linkplain Datum Datum},{@linkplain Datum Datum}&gt;</code>.
	 *
	 * @param node  the datum
	 * @return a view as a proper list
	 * @throws IllegalArgumentException if the list
	 *    terminator is not nil; i.e. improper list
	 * @since 0.1.0
	 */
	public static DatumList
	asProperList(Datum node)
	{
		return new DatumList(node).requireProperTail();
	}

	/**
	 * {@return the car of the given datum}
	 *
	 * @param node  the given datum
	 * @throws IllegalArgumentException if not a cons
	 * @since 0.1.0
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
	 * @since 0.1.0
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
	 * @since 0.1.0
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
	 * Copies a list wrapped in a list view.
	 * The list is properly terminated with nil.
	 *
	 * @param list  the source list
	 * @return a copy of the list wrapped in a list datum view
	 * @throws NullPointerException if the list or any element is null
	 * @since 0.1.0
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
	 *
	 * The returned view conforms to the interface
	 * <code>{@linkplain Map Map}&lt;{@linkplain Datum Datum},{@linkplain Datum Datum}&gt;</code>.
	 * On top of being a proper list with a nil-terminator,
	 * each element must be a cons whose car is the key
	 * and whose cdr is the value.  First car-key wins.
	 *
	 * @param node  datum
	 * @throws IllegalArgumentException if the list
	 *    is not a proper list, or the list consists
	 *    of any non-cons datum
	 * @throws NullPointerException if {@code node} is null
	 * @since 0.1.0
	 */
	public static AssocList
	asAlist(Datum node)
	{
		return new AssocList(node);
	}

	/**
	 * Copies a map wrapped in an associative map view.
	 * Entries become cons pairs in the iteration order of
	 * the source map’s entry set.  Chain this call with
	 * <code>.{@linkplain AssocList#head() head()}</code>
	 * if you just need the datum for serialization.
	 *
	 * @param map  the source map
	 * @return a copy of the map wrapped in an associative map datum view
	 * @throws NullPointerException if the map, any key, or any value is null
	 * @since 0.1.0
	 */
	public static AssocList
	fromMap(Map<? extends Datum, ? extends Datum> map)
	{
		return new AssocList(map);
	}
}
