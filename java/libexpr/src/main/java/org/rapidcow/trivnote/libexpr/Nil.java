package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;

/**
 * The atom devoid of value and the empty list.
 */
public enum Nil implements Datum, Iterable<Datum>
{
	/** The singleton that represents {@code nil}. */
	INSTANCE;

	/**
	 * {@return a list iterator for this}
	 */
	@Override public Iterator<Datum> iterator()
	{
		return new Walk(this);
	}
}
