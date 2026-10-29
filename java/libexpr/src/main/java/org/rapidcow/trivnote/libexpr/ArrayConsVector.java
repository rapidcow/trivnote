/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * A cons vector backed by a Java list, starting at an offset.
 * Cells made by {@link #cdr()} share the list.
 */
final
class
	ArrayConsVector
extends
	ConsVector
{
	private final List<Datum> vec;
	private final int off;

	/**
	 * Constructs a cell over {@code vec} starting at {@code off}.
	 * The list is not copied, so it must not change afterwards.
	 *
	 * @param vec  the elements
	 * @param off  index of the first element of this cell
	 */
	ArrayConsVector(List<Datum> vec, int off)
	{
		this.vec = vec;
		this.off = off;
	}

	/**
	 * Constructs a vector from a copy of the list.
	 *
	 * @param list  the elements, which must not be empty
	 * @throws NullPointerException on null {@code list}
	 * @throws IllegalArgumentException if {@code list} is empty
	 */
	ArrayConsVector(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Empty vector");
		}
		this.vec = new ArrayList<>(list);
		this.off = 0;
	}

	ArrayConsVector(Deque<? extends Datum> q, Cons p)
	{
		if (p.cdr() != Nil.ALT_PAREN) {
			throw new IllegalArgumentException(
				"Terminator is not #()"
			);
		}
		List<Datum> vec = new ArrayList<>();
		while (!q.isEmpty()) {
			vec.add(q.removeLast());
		}
		vec.add(p.car());
		this.vec = vec;
		this.off = 0;
	}

	@Override
	public final long
	count()
	{
		return vec.size() - off;
	}

	@Override
	public final Datum
	get(long index)
	{
		Objects.checkIndex(index, count());
		return vec.get(off + (int) index);
	}

	@Override
	public final Datum
	cdr()
	{
		int off1 = 1 + off;
		return off1 < vec.size() ? new ArrayConsVector(vec, off1) : Nil.ALT_PAREN;
	}
}
