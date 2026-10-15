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
	ListIterator<Datum>
{
	private final Datum head;
	private       Datum rest;
	private  int  cur;  /* "current" position */

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
	 * @since %v
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
	 *
	 * @since %v
	 */
	@Override
	public int
	nextIndex()
	{
		return 1 + cur;
	}

	/*
	 * Now this bit is the ugly, inefficient bit.
	 *
	 * We don't really need backward iteration (we can't, for that
	 * matter, as Lisp lists are singly lists); AbstractSequentialList
	 * does not even call previous().  But because we are not allowed
	 * to throw UnsupportedOperationException, I guess we're gonna
	 * do it.  Be warned though: previous() is almost always O(n).
	 *
	 * So DON'T DO IT!  Convert it into a real doubly linked list
	 * if you want bidirectional iteration!
	 */

	/**
	 * Seek to an arbitrary relative position by offset.
	 *
	 * @param off  offset relative to the current position
	 * @return datum at given position
	 * @throws NoSuchElementException  if you went too far
	 * @since %v
	 */
	public Datum
	seek(int off)
	{
		int index;
		if (off < 0) {
			index = cur + off;
			if (index < 0) {
				throw new NoSuchElementException();
			}
			/* RESET */
			rest = head;
			cur = 0;
		} else {
			index = off;
		}
		for (int pos = 0; pos < index; ++pos) {
			if (rest instanceof Cons cell) {
				rest = cell.cdr();
				++cur;
			} else {
				throw new NoSuchElementException();
			}
		}
		/* assert(index == pos); */
		return rest;
	}

	/**
	 * {@return the previous datum}
	 *
	 * @throws NoSuchElementException on start of list
	 * @implNote This operation is linear in the number
	 *    of datums preceding this datum.
	 * @since %v
	 */
	@Override
	public Datum
	previous()
	{
		return seek(-1);
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
	 * @since %v
	 */
	@Override
	public int
	previousIndex()
	{
		return cur - 1;
	}

	/**
	 * Not implemented.
	 *
	 * @throws UnsupportedOperationException always
	 * @since %v
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
	 * @since %v
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
	 * @since %v
	 */
	@Override
	public void
	add(Datum e)
	{
		throw new UnsupportedOperationException("Read-only");
	}
}
