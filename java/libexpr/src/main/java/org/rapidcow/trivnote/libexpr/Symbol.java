package org.rapidcow.trivnote.libexpr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A symbol is an interned atom.  It is also
 * unquoted in wire s-expression representation.
 */
public final class Symbol extends Atom
{
	/**
	 * Global symbol pool.
	 */
	private static final Map<String, Symbol> pool
		= new ConcurrentHashMap<>();

	private Symbol(String value)
	{
		super(value);
	}

	public static Symbol of(String value)
	{
		if (value == null) {
			throw new NullPointerException("value of a Symbol must not be null");
		}
		return pool.computeIfAbsent(value, Symbol::new);
	}
}
