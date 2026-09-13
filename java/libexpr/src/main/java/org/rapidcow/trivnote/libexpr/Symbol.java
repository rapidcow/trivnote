package org.rapidcow.trivnote.libexpr;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A symbol is an interned atom.  It is also
 * unquoted in wire s-expression representation.
 */
public final class Symbol extends Atom
{
	/**
	 * Global symbol map.
	 */
	private static final Map<String, Symbol> pool
		= new ConcurrentHashMap<>();

	public Symbol(String value)
	{
		super(value);
	}

	public static Symbol of(String value)
	{
		try {
			return pool.get(value);
		}
		catch (NoSuchElementException e) {
			Symbol atom = new Symbol(value);
			pool.put(value, atom);
			return atom;
		}
	}
}
