package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;

/**
 * A cons cell is a pair of datums.
 */
public final class Cons implements Datum, Iterable<Datum>
{
	/**
	 * First half of the pair.
	 */
	Datum car;

	/**
	 * Second half of the pair.
	 */
	Datum cdr;

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
		return car;
	}

	@Override public Iterator<Datum> iterator()
	{
		return new Walk(this);
	}
}
