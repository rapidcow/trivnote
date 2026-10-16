/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Deque;
import java.util.Iterator;

public final
class
	ConsList
extends
	Cons
{
	private final Datum car;
	private final Datum cdr;
	private final Datum end;
	private final int size;

	/* Used by DataLoader makeList */
	ConsList(Deque<Datum> q, Datum e)
	{
		if (q.isEmpty()) {
			throw new IllegalArgumentException("empty q");
		}

		size = q.size();
		end = e;

		Cons c = new ConsCell(q.pop(), e);
		while (!q.isEmpty()) {
			c = new ConsCell(q.pop(), c);
		}
		car = c.car();
		cdr = c.cdr();
	}

	@Override
	public Datum
	car()
	{
		return car;
	}

	@Override
	public Datum
	cdr()
	{
		return cdr;
	}

	public Datum
	tail()
	{
		return end;
	}

	public int
	size()
	{
		return size;
	}
}
