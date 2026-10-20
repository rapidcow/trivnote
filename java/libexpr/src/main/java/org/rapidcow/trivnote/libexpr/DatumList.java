/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.AbstractSequentialList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * List view into a datum.
 *
 * @since %v
 */
public final
class
	DatumList
extends
	AbstractSequentialList<Datum>
implements
	DatumView,
	Iterable<Datum>
{
	private final Datum head;
	private volatile Datum tail;
	/*
	 * Note!  size is only meaningful once tail is computed.
	 * tail is volatile and acts as a "ready" signal, so a
	 * non-null tail guarantees that the size is ready too.
	 *
	 * For this thread lock-ish mechanism to be useful though,
	 * modify size BEFORE a non-null tail is written.
	 */
	private long size = -1;

	/**
	 * Construct a list view.  All datums are
	 * valid, though possibly improper lists.
	 *
	 * @param list  source datum
	 */
	public
	DatumList(Datum list)
	{
		head = list;
		if (list instanceof ConsList cell) {
			size = cell.count();
			tail = cell.tail();
		}
	}

	public
	DatumList(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			head = Nil.INSTANCE;
			size = 0;
			tail = Nil.INSTANCE;
			return;
		}
		ConsList cList = Cons.fromList(list);
		head = cList;
		size = cList.count();
		tail = cList.tail();
	}

	/**
	 * {@return the underlying source datum}
	 */
	@Override
	public final Datum
	head()
	{
		return head;
	}

	/**
	 * {@return the list terminator}  The list
	 * terminator is the final atom in the cons
	 * chain.
	 */
	public final Datum
	tail()
	{
		if (tail == null) {
			finish(new DatumWalk(head));
		}
		return tail;
	}

	public void
	finish(DatumWalk iter)
	{
		if (iter.head() != head) {
			throw new IllegalArgumentException("not my kid");
		}

		for (;;) {
			try {
				iter.next();
			} catch (NoSuchElementException e) {
				break;
			}
		}

		/*
		 * size is considered "ready" by tail() and count().
		 * as soon as we write tail; so do it last!
		 */
		size = iter.position();
		tail = iter.rest();
	}

	/**
	 * {@return whether this list is proper}
	 * A list is proper if and only if its terminator
	 * {@link #tail()} is nil.
	 *
	 * <p>
	 * If you are doing this as a part of validation,
	 * consider the stricter {@link requireProperTail}.
	 */
	public final boolean
	isProperList()
	{
		return tail() == Nil.INSTANCE;
	}

	/**
	 * Require this list be nil-terminated.
	 *
	 * @throws IllegalArgumentException if this list is not proper
	 * @return this
	 */
	public final DatumList
	requireProperTail()
	{
		if (!isProperList()) {
			throw new IllegalArgumentException("Improper tail");
		}
		return this;
	}

	/**
	 * {@return a view of the same elements, terminated by nil}
	 * If this list is already proper, this is returned as is;
	 * otherwise the elements are copied into a new chain.
	 *
	 * @throws IllegalStateException if you have a mega list
	 *    with length exceeding {@link Integer#MAX_VALUE};
	 *    I think this should be rare, but still be careful!
	 * @since %v
	 */
	public DatumList
	toProperList()
	{
		/* Trigger the side-effect of computing list size. */
		if (isProperList()) {
			return this;
		}
		/* Observe racy field now! */
		long mySize = size;
		if (mySize == 0) {
			return new DatumList(Nil.INSTANCE);
		}
		if (mySize > Integer.MAX_VALUE) {
			throw new IllegalStateException(
				"Sorry, I won't convert lists whose "
				+ "length exceeds Integer.MAX_VALUE. "
				+ "Please do it yourself with a data "
				+ "structure of your choice."
			);
		}
		int sz = (int) mySize;
		/* ConsList pops q from the front, so push in order. */
		Deque<Datum> q = new ArrayDeque<>(sz);
		for (Datum node : this) {
			q.push(node);
		}
		Datum s = q.pop();
		return new DatumList(new ConsList(q, s, Nil.INSTANCE));
	}

	@Override
	public ListIterator<Datum>
	listIterator(int index)
	{
		DatumWalk walk = new DatumWalk(head);
		walk.seek(index);
		return walk;
	}

	@Override
	public final int
	size()
	{
		return (int) Math.min(count(), Integer.MAX_VALUE);
	}

	public final long
	count()
	{
		if (tail == null) {
			finish(new DatumWalk(head));
		}
		return size;
	}

	@Override
	public final boolean
	isEmpty()
	{
		if (Datum.isAtom(head)) {
			return true;
		}
		return super.isEmpty();
	}
}
