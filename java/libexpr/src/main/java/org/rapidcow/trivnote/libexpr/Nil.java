package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;

public enum Nil implements Datum, Iterable<Datum>
{
	INSTANCE;

	@Override public Iterator<Datum> iterator()
	{
		return new Walk(this);
	}
}
