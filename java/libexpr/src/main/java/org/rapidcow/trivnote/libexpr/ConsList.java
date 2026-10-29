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

/**
 * Cons cell with a known length.
 *
 * @since 0.1.0
 */
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

	/**
	 * Creates a cons chain prefixed with a deque.
	 * Elements are popped from the front and prepended
	 * one at a time, reversing their iteration order.
	 * This drains the deque and is thus destructive!
	 * <p>
	 * It is recommended that caller ensures {@code q}
	 * contains at least one element, or the wrapper
	 * becomes a bit useless. :(
	 *
	 * @param q  the deque
	 * @param p  base cons chain
	 * @throws NullPointerException if any argument or
	 *    deque element is {@code null}
	 * @see Cons#of(Deque, Cons)
	 */
	public
	ConsList(Deque<? extends Datum> q, Cons p)
	{
		Objects.requireNonNull(q, "q");
		Objects.requireNonNull(p, "p");
		Datum tail = p.cdr();
		long n;
		if (!(tail instanceof Cons)) {
			n = 1;
			end = tail;
		} else {
			DatumList list = new DatumList(p);
			n = list.count();
			end = list.tail();
		}
		while (!q.isEmpty()) {
			p = new ConsCell(q.pop(), p);
			++n;
		}
		car = p.car();
		cdr = p.cdr();
		size = n;
	}

	/**
	 * Creates a non-empty chain with implicit terminator.
	 * The last datum is used as the terminator.
	 *
	 * @param first  first element
	 * @param second  second element; also the terminator
	 *    if {@code etc} is empty
	 * @param etc  remaining elements, ending with the
	 *    terminator if non-empty
	 *
	 * @throws NullPointerException if any argument or
	 *    array element is {@code null}
	 */
	public
	ConsList(Datum first, Datum second, Datum... etc)
	{
		Objects.requireNonNull(first, "first");
		Objects.requireNonNull(second, "second");
		car = first;
		size = 1L + etc.length;
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
			c = Cons.of(second, etc[0]);
		}
		else {
			c = Cons.of(etc[etc.length - 2], etc[etc.length - 1]);
			for (int i = etc.length - 2; i > 0; --i) {
				c = new ConsCell(etc[i - 1], c);
			}
			c = new ConsCell(second, c);
		}
		cdr = c;
		end = etc[etc.length - 1];
	}

	/**
	 * Creates a non-empty chain with explicit terminator.
	 *
	 * @param first  first element
	 * @param etc  remaining elements
	 * @param tail  list terminator
	 *
	 * @throws NullPointerException if any argument or
	 *    array element is {@code null}
	 */
	public
	ConsList(Datum first, Datum[] etc, Datum tail)
	{
		Objects.requireNonNull(first, "first");
		Objects.requireNonNull(tail, "tail");
		car = first;
		end = tail;
		size = 1L + etc.length;
		if (etc.length == 0) {
			cdr = tail;
			return;
		}
		for (int i = 0; i < etc.length; ++i) {
			final int I = i;
			Objects.requireNonNull(etc[i], () -> "etc[" + I + "]");
		}
		Cons c = Cons.of(etc[etc.length - 1], tail);
		for (int i = etc.length - 1; i > 0; --i) {
			c = new ConsCell(etc[i - 1], c);
		}
		cdr = c;
	}

	/**
	 * Creates a proper cons chain from a non-empty list.
	 *
	 * @param list  source elements in iteration order
	 *
	 * @throws IllegalArgumentException if {@code list}
	 *    is empty
	 * @throws NullPointerException if {@code list} or
	 *    any element is {@code null}
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
		c = new ConsTail(p);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Datum
	car()
	{
		return car;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Datum
	cdr()
	{
		return cdr;
	}

	/**
	 * {@return the list terminator}
	 */
	public final Datum
	tail()
	{
		return end;
	}

	/**
	 * {@return the list size}
	 * If the length is too great to fit in a signed
	 * 32-bit int, return {@link Integer#MAX_VALUE}.
	 *
	 * @see #count()
	 */
	public final int
	size()
	{
		return (int) Math.min(size, Integer.MAX_VALUE);
	}

	/**
	 * {@return the list size}
	 */
	public final long
	count()
	{
		return size;
	}
}
