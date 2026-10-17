/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public final
class
	ConsList
extends
	Cons
{
	private final Datum car;
	private final Datum cdr;
	private final Datum end;
	private final long size;

	/* Used by DataLoader makeList */
	ConsList(Deque<Datum> q, Datum s, Datum e)
	{
		size = 1 + q.size();
		end = e;

		Cons c = new ConsCell(s, e);
		while (!q.isEmpty()) {
			c = new ConsCell(q.pop(), c);
		}
		car = c.car();
		cdr = c.cdr();
	}

	/**
	 * Creates a non-empty chain with the last datum as its tail.
	 *
	 * @throws NullPointerException if any argument or array element is null
	 */
	public
	ConsList(Datum first, Datum second, Datum... etc)
	{
		Objects.requireNonNull(first, "first");
		Objects.requireNonNull(second, "second");
		car = first;
		size = 1 + etc.length;
		if (etc.length == 0) {
			cdr = second;
			end = second;
			return;
		}
		for (int i = 0; i < etc.length; ++i) {
			final int I = i;
			Objects.requireNonNull(etc[i], () -> "etc[" + I + "]");
		}
		Cons c;
		if (etc.length == 1) {
			c = new ConsCell(second, etc[0]);
		}
		else {
			c = new ConsCell(etc[etc.length - 2], etc[etc.length - 1]);
			for (int i = etc.length - 2; i > 0; --i) {
				c = new ConsCell(etc[i - 1], c);
			}
			c = new ConsCell(second, c);
		}
		cdr = c;
		end = etc[etc.length - 1];
	}

	/**
	 * Creates a non-empty chain with the given tail; {@code etc} may be empty.
	 *
	 * @throws NullPointerException if any argument or array element is null
	 */
	public
	ConsList(Datum first, Datum[] etc, Datum tail)
	{
		Objects.requireNonNull(first, "first");
		Objects.requireNonNull(tail, "tail");
		car = first;
		end = tail;
		size = 1 + etc.length;
		if (etc.length == 0) {
			cdr = tail;
			return;
		}
		for (int i = 0; i < etc.length; ++i) {
			final int I = i;
			Objects.requireNonNull(etc[i], () -> "etc[" + I + "]");
		}
		Cons c = new ConsCell(etc[etc.length - 1], tail);
		for (int i = etc.length - 1; i > 0; --i) {
			c = new ConsCell(etc[i - 1], c);
		}
		cdr = c;
	}

	/**
	 * Creates a cons from a non-empty list.
	 *
	 * @throws IllegalArgumentException if {@code list} is empty
	 * @throws NullPointerException if {@code list} or any element is null
	 */
	public
	ConsList(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Empty list");
		}

		Cons c;
		int sz = list.size();
		size = sz;
		end = Nil.INSTANCE;
		if (list instanceof DatumList sList) {
			/*
			 * Reuse the chain when it is already proper;
			 * an improper one is rebuilt with a nil tail,
			 * as the List contract only covers elements.
			 */
			c = (Cons) sList.toProperList().head();
			car = c.car();
			cdr = c.cdr();
			return;
		}

		/*
		 * The rest of Java collections can walk
		 * backwards.  So I'll just do that...
		 */
		ListIterator<? extends Datum> iter
			= list.listIterator(sz);
		Datum p = Objects.requireNonNull(
			iter.previous(),
			() -> "list[" + (size - 1) + "]"
		);
		int i = sz - 1;
		c = new ConsCell(p, Nil.INSTANCE);
		while (iter.hasPrevious()) {
			final int I = i;
			p = Objects.requireNonNull(
				iter.previous(),
				() -> "list[" + (I - 1) + "]"
			);
			c = new ConsCell(p, c);
			--i;
		}
		car = c.car();
		cdr = c.cdr();
	}

	@Override
	public final Datum
	car()
	{
		return car;
	}

	@Override
	public final Datum
	cdr()
	{
		return cdr;
	}

	public final Datum
	tail()
	{
		return end;
	}

	public final int
	size()
	{
		return (int) Math.min(size, Integer.MAX_VALUE);
	}

	public final long
	count()
	{
		return size;
	}
}
