package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;

/**
 * A cons cell is a pair of datums.  A chain of
 * cons cells recursively is considered a list.
 */
public final class Cons implements Datum, Iterable<Datum>
{
	/** Former half of the pair. */
	final Datum car;

	/** Latter half of the pair. */
	final Datum cdr;

	public Cons(Datum car, Datum cdr)
	{
		this.car = car;
		this.cdr = cdr;
	}

	public Datum car()
	{
		return car;
	}

	public Datum cdr()
	{
		return cdr;
	}

	@Override public Iterator<Datum> iterator()
	{
		return new Walk(this);
	}
}
