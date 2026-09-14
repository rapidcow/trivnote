package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * List iterator.
 */
public final class Walk implements Iterator<Datum>
{
	private Datum tail;

	public Walk(Datum node)
	{
		this.tail = node;
	}

	@Override public Datum next()
	{
		if (tail instanceof Cons cell) {
			tail = cell.cdr();
			return cell.car();
		}
		throw new NoSuchElementException();
	}

	@Override public boolean hasNext()
	{
		return tail instanceof Cons;
	}

	/**
	 * The cdr of the current iteration.
	 * At the end of iteration, this is nil for a proper
	 * list and a non-nil atom for an improper list.
	 */
	public Datum tail()
	{
		return tail;
	}
}
