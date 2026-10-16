/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A pair of {@link Datum}s.
 * A chain of cons cells created by recursively
 * appending to cdr forms a list.
 *
 * <p>
 * If the list terminates with a meaningless datum
 * (i.e., nil), it is considered <em>proper</em>.
 * It is otherwise considered <em>improper</em>.
 * Cons cells are {@link Iterable} as a list.
 */
public non-sealed
abstract class
	Cons
implements
	Datum
{
	public abstract Datum car();
	public abstract Datum cdr();

	/**
	 * Creates a nonempty chain with the last datum as its tail.
	 *
	 * @throws NullPointerException if any argument or array element is null
	 */
	public static Cons
	of(Datum car, Datum cdr, Datum... etc)
	{
		if (etc.length == 0) {
			return new ConsCell(car, cdr);
		}
		return new ConsList(car, cdr, etc);
	}

	/**
	 * Creates a nonempty chain with the given tail; {@code etc} may be empty.
	 *
	 * @throws NullPointerException if any argument or array element is null
	 */
	public static Cons
	of(Datum car, Datum[] etc, Datum tail)
	{
		if (etc.length == 0) {
			return new ConsCell(car, tail);
		}
		return new ConsList(car, etc, tail);
	}

	/**
	 * Creates a nonempty proper list; {@code etc} may be empty.
	 *
	 * @throws NullPointerException if any argument or array element is null
	 */
	public static Cons
	ofList(Datum car, Datum... etc)
	{
		if (etc.length == 0) {
			return new ConsCell(car, Nil.INSTANCE);
		}
		return new ConsList(car, etc, Nil.INSTANCE);
	}

	/**
	 * Creates a cons from a nonempty list.
	 *
	 * @throws IllegalArgumentException if {@code list} is empty
	 * @throws NullPointerException if {@code list} or any element is null
	 */
	public static Cons
	fromList(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Empty list");
		}
		return new ConsList(list);
	}

	private int myHash;

	@Override
	public boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}
		if (o instanceof Cons cell) {
			return this.car().equals(cell.car())
				&& this.cdr().equals(cell.cdr());
		}
		return false;
	}

	@Override
	public int
	hashCode()
	{
		if (myHash == 0) {
			myHash = Objects.hash(car(), cdr());
		}
		return myHash;
	}
}
