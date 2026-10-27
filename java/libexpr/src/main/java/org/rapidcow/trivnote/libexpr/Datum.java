/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/*
 * Note: these imports are for javadoc
 */
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Basic unit of an S-expression.
 * A datum is either {@link Nil}, {@link Cons}, {@link Symbol},
 * {@link BoolScalar}, {@link NumberScalar}, or {@link StringScalar}
 *
 * <p>
 * Note that {@link Nil} is both an <q>{@linkplain #isAtom(Datum) atom}</q>
 * and a <q>{@linkplain #isList(Datum) list}</q>.
 *
 * <p>
 * Datums <em>must</em> implement
 * <code>{@linkplain Object#hashCode() hashCode()}</code> and
 * <code>{@linkplain Object#equals(Object) equals(Object)}</code>
 * in a way such that they are compared by value, not by
 * object identity.  Datums <em>should</em> implement
 * a reasonably descriptive string representation by
 * overriding {@link Object#toString()}, though this is not
 * mandatory.
 */
public sealed
interface
	Datum
permits
	BoolScalar,
	Cons,           /* non-sealed */
//	DatumVector
	Nil,
	NumberScalar,   /* non-sealed */
//	OctetVector
	StringScalar,
	Symbol
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
		return node == Nil.INSTANCE || node instanceof Cons;
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
	 * Copies a proper list from a {@link List}.
	 * The list is properly terminated with nil.
	 *
	 * @param list  the source list
	 * @return a copy of the list as a datum
	 * @throws NullPointerException if the list or
	 *    any element is {@code null}
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
	 * Constructs a proper list from a {@link Deque}.
	 * Elements are popped from the front and prepended
	 * one at a time, reversing their iteration order.
	 * This drains the deque and is thus destructive!
	 *
	 * @param q  the deque
	 * @return a copy of the deque as a datum
	 * @throws NullPointerException if {@code q} or
	 *    any deque element is {@code null}
	 * @since 0.1.0
	 */
	public static Datum
	ofDeque(Deque<? extends Datum> q)
	{
		if (q.isEmpty()) {
			return Nil.INSTANCE;
		}
		return Cons.of(q, Cons.ofList(q.pop()));
	}

	/**
	 * Returns the datum as an associative list (alist).
	 *
	 * The returned view conforms to the interface
	 * <code>{@linkplain Map Map}&lt;{@linkplain Datum Datum},{@linkplain Datum Datum}&gt;</code>.
	 * On top of being a proper list with a nil-terminator,
	 * each element must be a cons whose car is the key
	 * and whose cdr is the value.  First car-key wins.
	 *
	 * @param node  datum
	 * @return the datum as an alist
	 * @throws IllegalArgumentException if the list
	 *    is not a proper list, or the list consists
	 *    of any non-cons datum
	 * @throws NullPointerException if {@code node} is
	 *    {@code null}
	 * @since 0.1.0
	 */
	public static AssocList
	asAlist(Datum node)
	{
		return new AssocList(node);
	}

	/**
	 * Copies a {@link Map} into an alist.
	 * Entries become cons pairs in the iteration order of
	 * the source map’s entry set.
	 *
	 * @param map  the source map
	 * @return a copy of the map as an alist
	 * @throws NullPointerException if the map,
	 *    any key, or any value is {@code null}
	 * @since 0.1.0
	 */
	public static Datum
	fromMap(Map<? extends Datum, ? extends Datum> map)
	{
		return new AssocList(map).head();
	}
}
