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
import java.util.Objects;

/**
 * List view into a datum.  Elements of a list
 * are the cars along its cdr chain.  The final
 * non-cons datum (known as the <em>terminator</em>)
 * is accessible at {@link #tail()}.
 *
 * <p>
 * The terminator is not considered an element
 * of the list, so Java list operations such as
 * equality and hash code are not affected by
 * its value.
 *
 * <p>
 * {@code DatumList} is read-only because datums
 * are immutable.
 *
 * @since 0.1.0
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
	 * @throws NullPointerException on null {@code list}
	 */
	public
	DatumList(Datum list)
	{
		head = Objects.requireNonNull(list);
		if (list instanceof ConsList cell) {
			size = cell.count();
			tail = cell.tail();
		}
	}

	/**
	 * Creates a proper datum list from another list.
	 * Note that improper lists will not round-trip,
	 * due to the improper tail being replaced with nil.
	 *
	 * @param list  source elements in iteration order
	 * @throws NullPointerException if {@code list}
	 *    or any element is {@code null}
	 */
	public
	DatumList(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			head = Nil.INSTANCE;
			size = 0;
			tail = Nil.INSTANCE;
			return;
		}
		ConsList cList = new ConsList(list);
		head = cList;
		size = cList.count();
		tail = cList.tail();
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

	/**
	 * Consumes a datum iterator and updates this list’s
	 * length and terminator from its states.  It is ideal
	 * if the iterator has been partially or fully consumed.
	 *
	 * <p>Of course, the iterator has to be iterating from
	 * the same root datum as the one this list is viewing.
	 *
	 * @param iter  iterator
	 * @throws NullPointerException if iter is null
	 * @throws IllegalArgumentException if the iterator
	 *    is iterating from a different root datum
	 */
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
	 * consider the stricter {@link #requireProperTail()}.
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
	 * @since 0.1.0
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
		return new DatumList(new ConsList(q, new ConsTail(s)));
	}

	/**
	 * Creates a read-only iterator positioned before the given element.
	 *
	 * @param index  starting position, from zero through the list size
	 * @return a new iterator
	 * @throws IndexOutOfBoundsException if index is outside that range
	 * @implNote Reaching the starting position takes linear time. Backward
	 *    iteration rescans from the head; forward iteration follows cdr links.
	 */
	@Override
	public DatumWalk
	listIterator(int index)
	{
		DatumWalk walk = new DatumWalk(head);
		walk.seek(index);
		return walk;
	}

	/**
	 * {@return the element count, capped at {@link Integer#MAX_VALUE}}
	 * The terminator is excluded.
	 * @see #count()
	 */
	@Override
	public final int
	size()
	{
		return (int) Math.min(count(), Integer.MAX_VALUE);
	}

	/**
	 * {@return the element count, excluding the terminator}
	 * The count and terminator are computed together and cached.
	 *
	 * @implNote The first call walks the chain unless its length is already known.
	 */
	public final long
	count()
	{
		if (tail == null) {
			finish(new DatumWalk(head));
		}
		return size;
	}

	/**
	 * {@return whether the source has no cons cells}
	 * Both nil and a non-nil atom yield an empty view.
	 */
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
