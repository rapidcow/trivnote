/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Datum list iterator.
 */
public final
class
	DatumWalk
implements
	DatumView,
	ListIterator<Datum>
{
	private final Datum head;
	private       Datum rest;
	private long cur;  /* "current" position */

	/**
	 * Creates a list iterator.
	 *
	 * @param node  starting datum
	 * @throws NullPointerException on null {@code node}
	 */
	public
	DatumWalk(Datum node)
	{
		head = Objects.requireNonNull(node, "node must not be null");
		rest = head;
	}

	/**
	 * {@return the root datum}
	 *
	 * @since 0.1.0
	 */
	public final Datum
	head()
	{
		return head;
	}

	/**
	 * {@return the datum of the current iteration}
	 * At the end of iteration, this is nil for a proper
	 * list and a non-nil atom for an improper list.
	 */
	public final Datum
	rest()
	{
		return rest;
	}

	/**
	 * {@return the current position}
	 *
	 * @since 0.1.0
	 */
	public final long
	position()
	{
		return cur;
	}

	/**
	 * {@return the next datum}
	 *
	 * @throws NoSuchElementException on end of list
	 */
	@Override
	public Datum
	next()
	{
		/*
		 * Because this is the more common operation (and,
		 * IMHO, more "sane"), we'll implement this without
		 * any funny loops the forsaken freaks below use.
		 */
		if (rest instanceof Cons cell) {
			rest = cell.cdr();
			++cur;
			return cell.car();
		}
		throw new NoSuchElementException();
	}

	/**
	 * {@return whether there is a next datum}
	 */
	@Override
	public boolean
	hasNext()
	{
		return rest instanceof Cons;
	}

	/**
	 * {@return index of the next datum}
	 * This value is truncated at the 32-bit integer maximum.
	 * For full resolution, use <code>{@linkplain #position position()}</code>.
	 *
	 * @since 0.1.0
	 */
	@Override
	public int
	nextIndex()
	{
		return (int) Math.min(cur, Integer.MAX_VALUE);
	}

	/*
	 * Now this bit is the ugly, inefficient bit.
	 *
	 * We don't really need backward iteration (we can't, for that
	 * matter, as Lisp lists are singly-linked lists); Abstract-
	 * SequentialList does not even call previous().  But because
	 * we are not allowed to throw UnsupportedOperationException,
	 * I guess we're gonna do it anyway....  >_>
	 * Be warned though: previous() is almost always O(n).
	 *
	 * So DON'T DO IT!  Convert it into a real doubly linked list
	 * if you want bidirectional iteration!
	 */

	/**
	 * Seek to an arbitrary relative position by offset.
	 *
	 * @param off  offset relative to the current position
	 * @return sub-list starting at given position
	 * @throws IndexOutOfBoundsException  if you went too far
	 * @since 0.1.0
	 */
	public Datum
	seek(long off)
	{
		/*
		 * Fork iterator states (cur, rest) => (pos, node),
		 * so an out-of-bounds seek would not leave us in a
		 * weird place.
		 */
		long pos = cur;
		Datum node = rest;

		/*
		 * Now compute an index, relative to either the head
		 * (if offset is negative) or to here (if positive).
		 */
		long index;
		if (off < 0) {
			index = pos + off;
			if (index < 0) {
				throw new IndexOutOfBoundsException(
					"Effective index " + index + " out of bounds"
				);
			}
			/* RESET */
			node = head;
			pos = 0;
		} else {
			index = off;
		}

		for (long i = 0; i < index; ++i) {
			if (node instanceof Cons cell) {
				node = cell.cdr();
				++pos;
			} else {
				/* Compute what the absolute index would have been. */
				long effIndex = (long) (pos - i) + off;
				throw new IndexOutOfBoundsException(
					"Effective index " + effIndex + " out of "
					+ "bounds for length " + pos
				);
			}
		}

		/* assert(index == pos); */

		this.cur = pos;
		this.rest = node;
		return node;
	}

	/**
	 * {@return the previous datum}
	 *
	 * @throws NoSuchElementException on start of list
	 * @implNote This operation is linear in the number
	 *    of datums preceding this datum.
	 */
	@Override
	public Datum
	previous()
	{
		try {
			return Datum.car(seek(-1L));
		} catch (IndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}

	/**
	 * {@return whether there is a previous datum}
	 */
	@Override
	public boolean
	hasPrevious()
	{
		/* "pos > 0" should work too, but eh -- I don't like this pos thing. */
		return head != rest;
	}

	/**
	 * {@return index of the previous datum}
	 * This value is truncated at the 32-bit integer maximum.
	 * For full resolution, use <code>{@linkplain #position position()} - 1L</code>.
	 */
	@Override
	public int
	previousIndex()
	{
		return (int) Math.min(cur - 1L, Integer.MAX_VALUE);
	}

	/**
	 * Not implemented.
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void
	remove()
	{
		throw new UnsupportedOperationException("Read-only");
	}

	/**
	 * Not implemented.
	 *
	 * @param e  unused
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void
	set(Datum e)
	{
		throw new UnsupportedOperationException("Read-only");
	}

	/**
	 * Not implemented.
	 *
	 * @param e  unused
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void
	add(Datum e)
	{
		throw new UnsupportedOperationException("Read-only");
	}
}
