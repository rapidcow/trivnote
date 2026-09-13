package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Walk implements Iterator<Datum>
{
	private Datum tail;

	public Walk(Datum tail)
	{
		this.tail = tail;
	}

	@Override public Datum next()
	{
		if (tail instanceof Cons cell) {
			tail = cell.cdr;
			return cell.car;
		}
		throw new NoSuchElementException();
	}

	@Override public boolean hasNext()
	{
		return tail instanceof Cons;
	}

	public Datum tail()
	{
		return tail;
	}
}
