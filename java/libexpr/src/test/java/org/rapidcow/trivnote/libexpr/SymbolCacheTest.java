package org.rapidcow.trivnote.libexpr;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.rapidcow.trivnote.libexpr.Datum.car;
import static org.rapidcow.trivnote.libexpr.Datum.cdr;

/**
 * Tests {@link DataLoader#useSymbolCache}.
 */
class SymbolCacheTest
{
	@Test void testSharedWithinOneLoader()
	{
		Map<String,Symbol> cache = new HashMap<>();
		DataLoader l = new DataLoader(new StringReader("(a b a |a|)"))
			.useSymbolCache(cache);
		Datum d = l.next();
		Datum a1 = car(d);
		Datum a2 = car(cdr(cdr(d)));
		Datum a3 = car(cdr(cdr(cdr(d))));
		assertSame(a1, a2);
		assertSame(a1, a3);
		assertEquals(2, cache.size());
	}

	@Test void testSharedAcrossLoaders()
	{
		Map<String,Symbol> cache = new HashMap<>();
		Datum x = new DataLoader(new StringReader("a"))
			.useSymbolCache(cache).next();
		Datum y = new DataLoader(new StringReader("a"))
			.useSymbolCache(cache).next();
		assertSame(x, y);
	}

	@Test void testNoCacheNotIdentical()
	{
		Datum x = new DataLoader(new StringReader("a")).next();
		Datum y = new DataLoader(new StringReader("a")).next();
		assertNotSame(x, y);
		assertEquals(x, y);
	}

	@Test void testNullRemovesCache()
	{
		Map<String,Symbol> cache = new HashMap<>();
		DataLoader l = new DataLoader(new StringReader("a a"))
			.useSymbolCache(cache).useSymbolCache(null);
		assertNotSame(l.next(), l.next());
		assertEquals(0, cache.size());
	}

	@Test void testScalarsNotCached()
	{
		Map<String,Symbol> cache = new HashMap<>();
		new DataLoader(new StringReader("(1 \"x\" 2.5)"))
			.useSymbolCache(cache).next();
		assertEquals(0, cache.size());
	}
}
