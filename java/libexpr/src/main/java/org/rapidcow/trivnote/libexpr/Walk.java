/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * List iterator.
 */
public final
class
	Walk
implements
	Iterator<Datum>
{
	private Datum tail;

	/**
	 * Creates a list iterator.
	 *
	 * @param node  starting datum
	 * @throws NullPointerException on null {@code node}
	 */
	public
	Walk(Datum node)
	{
		this.tail = Objects.requireNonNull(node, "node must not be null");
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
		if (tail instanceof Cons cell) {
			tail = cell.cdr();
			return cell.car();
		}
		throw new NoSuchElementException();
	}

	/**
	 * {@return whether any datum remains}
	 */
	@Override
	public boolean
	hasNext()
	{
		return tail instanceof Cons;
	}

	/**
	 * {@return the datum of the current iteration}
	 * At the end of iteration, this is nil for a proper
	 * list and a non-nil atom for an improper list.
	 */
	public Datum
	tail()
	{
		return tail;
	}
}
