package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;

/**
 * Nil is the atom devoid of value and the empty list.
 */
public enum Nil implements Datum, Iterable<Datum>
{
	/** The singleton that represents nil. */
	INSTANCE;

	@Override public Iterator<Datum> iterator()
	{
		return new Walk(this);
	}
}
