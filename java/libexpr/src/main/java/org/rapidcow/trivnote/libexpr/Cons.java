/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;
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
	Datum,
	Iterable<Datum>
{
	public abstract Datum car();
	public abstract Datum cdr();

	public static Cons
	of(Datum car, Datum cdr)
	{
		return new ConsCell(car, cdr);
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
