/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.AbstractSequentialList;
import java.util.ArrayDeque;
import java.util.Deque;
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
	Iterable<Datum>
{
	private final Datum head;
	private       Datum tail;
	private long  size =  -1;

	public
	DatumList(Datum list)
	{
		head = list;
		if (list instanceof ConsList cell) {
			tail = cell.tail();
			size = cell.count();
		}
	}

	public final Datum
	head()
	{
		return head;
	}

	public final Datum
	tail()
	{
		if (tail == null) {
			DatumWalk iter = new DatumWalk(head);
			finish(iter);
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
		tail = iter.rest();
		size = iter.position();
	}

	public DatumList
	requireProperTail()
	{
		if (tail() != Nil.INSTANCE) {
			throw new IllegalArgumentException("Improper tail");
		}
		return this;
	}

	/**
	 * {@return a view of the same elements, terminated by nil}
	 * If this list is already proper, this is returned as is;
	 * otherwise the elements are copied into a new chain.
	 *
	 * @since %v
	 */
	public DatumList
	toProperList()
	{
		if (tail() == Nil.INSTANCE) {
			return this;
		}
		if (size == 0) {
			return new DatumList(Nil.INSTANCE);
		}
		if (size > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException("Very big list");
		}
		int sz = (int) size;
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
	public int
	size()
	{
		return (int) Math.min(count(), Integer.MAX_VALUE);
	}

	public long
	count()
	{
		if (size < 0) {
			tail();
		}
		return size;
	}
}
