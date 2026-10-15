/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.AbstractSequentialList;
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
	private  int  size =  -1;

	public
	DatumList(Datum list)
	{
		head = list;
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
		size = iter.nextIndex();
	}

	public DatumList
	requireProperTail()
	{
		if (tail() != Nil.INSTANCE) {
			throw new IllegalArgumentException("Improper tail");
		}
		return this;
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
		if (size < 0) {
			tail();
		}
		return size;
	}
}
