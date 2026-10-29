/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Random-access list view into a vector datum, which is either
 * {@link Nil#ALT_PAREN} (the empty vector) or a {@link ConsVector}.
 * Unlike {@link DatumList}, this view is strict: no other datum
 * is accepted, and nothing is ever copied from a cons chain.
 *
 * <p>
 * Like {@link OctetVector}, {@link #count()} and {@link #get(long)}
 * take the full range of {@code long}, while {@link #size()} and
 * {@link #get(int)} are there for the Java collections.
 *
 * @since %v
 */
public final
class
	DatumVector
extends
	AbstractList<Datum>
implements
	RandomAccess
{
	private final Datum head;
	/* null if and only if head is the empty vector. */
	private final ConsVector vec;

	/**
	 * Construct a vector view.
	 *
	 * @param vector  source datum
	 * @throws NullPointerException on null {@code vector}
	 * @throws IllegalArgumentException if {@code vector}
	 *   is neither {@link Nil#ALT_PAREN} nor a cons vector
	 */
	public
	DatumVector(Datum vector)
	{
		Objects.requireNonNull(vector);
		if (vector == Nil.ALT_PAREN) {
			vec = null;
		} else if (vector instanceof ConsVector cv) {
			vec = cv;
		} else {
			throw new IllegalArgumentException("Not a vector");
		}
		head = vector;
	}

	/**
	 * Construct a vector from a copy of the list.
	 *
	 * @param list  the elements
	 * @throws NullPointerException on null {@code list}
	 */
	public
	DatumVector(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			head = Nil.ALT_PAREN;
			vec = null;
		} else {
			vec = new ArrayConsVector(list);
			head = vec;
		}
	}

	/**
	 * {@return the underlying source datum}
	 */
	public final Datum
	head()
	{
		return head;
	}

	/**
	 * {@return the number of elements in this vector}
	 */
	public final long
	count()
	{
		return vec == null ? 0 : vec.count();
	}

	@Override
	public final int
	size()
	{
		return (int) Math.min(count(), Integer.MAX_VALUE);
	}

	@Override
	public final boolean
	isEmpty()
	{
		return vec == null;
	}

	/**
	 * {@return the element at the given index}
	 *
	 * @param index  the given index
	 * @throws IndexOutOfBoundsException if {@code index} is
	 *   negative or no less than {@link #count()}
	 */
	public final Datum
	get(long index)
	{
		Objects.checkIndex(index, count());
		return vec.get(index);
	}

	@Override
	public final Datum
	get(int index)
	{
		return get((long) index);
	}
}
