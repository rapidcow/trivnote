/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

	@Test void testImmutableMapKnownSymbolResolves()
	{
		Symbol known = Symbol.of("a");
		Map<String,Symbol> cache = Map.of("a", known);
		Datum d = new DataLoader(new StringReader("a"))
			.useSymbolCache(cache).next();
		assertSame(known, d);
	}

	@Test void testImmutableMapUnknownSymbolStillLoads()
	{
		Map<String,Symbol> cache = Map.of("a", Symbol.of("a"));
		Datum d = new DataLoader(new StringReader("zzz"))
			.useSymbolCache(cache).next();
		assertEquals(Symbol.of("zzz"), d);
		assertEquals(1, cache.size());
	}

	/*
	 * Many threads, each with its own loader, read the same many symbols
	 * through one shared concurrent map.  Whichever thread loses the race
	 * to cache a symbol must adopt the winner, so every thread has to end
	 * up with the same instance for each value.  The loss is only sometimes
	 * hit, but the assertion holds on every run if the code is correct.
	 */
	@Test void testConcurrentLoadersAgree()
	throws
		Exception
	{
		final int nthreads = 8;
		final int nsyms = 2000;
		StringBuilder src = new StringBuilder();
		for (int i = 0; i < nsyms; i++) {
			src.append("s").append(i).append(' ');
		}
		String text = src.toString();

		Map<String,Symbol> cache = new ConcurrentHashMap<>();
		CountDownLatch go = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(nthreads);
		try {
			List<Future< List<Datum> >> results = new ArrayList<>();
			for (int t = 0; t < nthreads; t++) {
				results.add(pool.submit(() -> {
					DataLoader l = new DataLoader(new StringReader(text))
						.useSymbolCache(cache);
					go.await();
					List<Datum> out = new ArrayList<>();
					l.forEachRemaining(out::add);
					return out;
				}));
			}
			go.countDown();

			List< List<Datum> > all = new ArrayList<>();
			for (Future< List<Datum> > f : results) {
				all.add(f.get());
			}
			assertEquals(nsyms, cache.size());
			for (int i = 0; i < nsyms; i++) {
				Symbol expected = cache.get("s" + i);
				for (List<Datum> one : all) {
					assertSame(expected, one.get(i));
				}
			}
		} finally {
			pool.shutdownNow();
		}
	}
}
