/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API tests for AssocList lookups, counting and entry iteration.
 */
@DisplayName("AssocList API tests")
class AssocListTest
{
	static final Datum A = Symbol.of("a");
	static final Datum B = Symbol.of("b");
	static final Datum C = Symbol.of("c");
	static final Datum Z = Symbol.of("z");

	static Cons pair(Datum key, Datum value)
	{
		return new ConsCell(key, value);
	}

	/* ((a . 1) (b . 2) (c . 3)) */
	static AssocList abc()
	{
		return new AssocList(Cons.ofList(
			pair(A, Scalar.of("1")),
			pair(B, Scalar.of("2")),
			pair(C, Scalar.of("3"))));
	}

	@Test void testEmptyList()
	{
		AssocList alist = new AssocList(Nil.INSTANCE);
		assertEquals(0, alist.size());
		assertEquals(0L, alist.count());
		assertTrue(alist.hasUniqueKeys());
		assertNull(alist.get(A));
		assertFalse(alist.containsKey(A));
	}

	@Test void testNullListIsRejected()
	{
		assertThrows(NullPointerException.class, () -> new AssocList(null));
	}

	@Test void testNonConsEntryIsRejected()
	{
		assertThrows(IllegalArgumentException.class,
			() -> new AssocList(Cons.ofList(A)));
		assertThrows(IllegalArgumentException.class,
			() -> new AssocList(Cons.ofList(pair(A, B), Z)));
		assertThrows(IllegalArgumentException.class,
			() -> new AssocList(Cons.ofList(pair(A, B), Nil.INSTANCE)));
	}

	@Test void testImproperListIsRejected()
	{
		assertThrows(IllegalArgumentException.class,
			() -> new AssocList(Z));
		assertThrows(IllegalArgumentException.class,
			() -> new AssocList(Cons.of(pair(A, B), Z)));
		assertThrows(IllegalArgumentException.class,
			() -> new AssocList(Cons.of(pair(A, B), pair(C, Z), Z)));
	}

	@Test void testGet()
	{
		AssocList alist = abc();
		assertEquals(Scalar.of("1"), alist.get(A));
		assertEquals(Scalar.of("2"), alist.get(B));
		assertEquals(Scalar.of("3"), alist.get(C));
		assertNull(alist.get(Z));
	}

	/* The value is the whole cdr, so (a b) maps a to (b), not to b. */
	@Test void testValueIsTheCdr()
	{
		Cons list = Cons.ofList(B, C);
		Datum entries = Cons.ofList(
			pair(A, list),
			Cons.ofList(B, C),
			Cons.ofList(C));
		AssocList alist = new AssocList(entries);
		assertSame(list, alist.get(A));
		assertEquals(Cons.ofList(C), alist.get(B));
		assertSame(Nil.INSTANCE, alist.get(C));
	}

	@Test void testFirstEntryShadowsLaterOnes()
	{
		Datum first = Scalar.of("first");
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, first),
			pair(B, Z),
			pair(A, Scalar.of("second"))));
		assertSame(first, alist.get(A));
		assertSame(Z, alist.get(B));
	}

	/* Keys match by equals(), not identity, and need not be atoms. */
	@Test void testStructuralKeys()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(Cons.ofList(A, B), C),
			pair(Scalar.of("k"), Z),
			pair(Nil.INSTANCE, A)));
		assertSame(C, alist.get(new ConsList(A, B, Nil.INSTANCE)));
		assertSame(Z, alist.get(Scalar.of("k")));
		assertSame(A, alist.get(Nil.INSTANCE));
		assertNull(alist.get(Cons.ofList(A)));
		assertNull(alist.get(Cons.of(A, B)));
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test void testForeignKeys()
	{
		AssocList alist = abc();
		assertNull(alist.get(null));
		assertNull(alist.get("a"));
		assertFalse(alist.containsKey(null));
		assertFalse(alist.containsKey("a"));
	}

	@Test void testContainsKey()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, B),
			pair(B, Nil.INSTANCE),
			pair(A, C)));
		assertTrue(alist.containsKey(A));
		/* A nil value is still a value. */
		assertTrue(alist.containsKey(B));
		assertFalse(alist.containsKey(C));
		assertFalse(alist.containsKey(Nil.INSTANCE));
	}

	@Test void testSizeOfUniqueKeys()
	{
		AssocList alist = abc();
		assertEquals(3, alist.size());
		assertEquals(3L, alist.count());
		assertTrue(alist.hasUniqueKeys());
	}

	@Test void testSizeCountsShadowedKeysOnce()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, B),
			pair(B, C),
			pair(A, C),
			pair(A, Z)));
		assertEquals(2, alist.size());
		assertEquals(2L, alist.count());
		assertFalse(alist.hasUniqueKeys());
	}

	@Test void testSizeCountsEqualKeysOnce()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(Cons.ofList(A, B), C),
			pair(new ConsList(A, B, Nil.INSTANCE), Z),
			pair(Scalar.of("k"), Z),
			pair(Scalar.of("k"), A)));
		assertEquals(2, alist.size());
		assertEquals(2L, alist.count());
		assertFalse(alist.hasUniqueKeys());
	}

	/* The count is cached; asking again must not change the answer. */
	@Test void testRepeatedQueriesAgree()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, B), pair(A, C), pair(B, C)));
		for (int i = 0; i < 3; ++i) {
			assertEquals(2, alist.size());
			assertEquals(2L, alist.count());
			assertFalse(alist.hasUniqueKeys());
		}
	}

	@Test void testLongList()
	{
		int n = 100_000;
		Datum entries = Nil.INSTANCE;
		for (int i = n - 1; i >= 0; --i) {
			Datum key = Scalar.of(Integer.toString(i % (n / 2)));
			entries = new ConsCell(pair(key, Scalar.of(Integer.toString(i))), entries);
		}
		AssocList alist = new AssocList(entries);
		assertEquals(n / 2, alist.size());
		assertFalse(alist.hasUniqueKeys());
		/* Key 0 appears at index 0 and n/2; the first one wins. */
		assertEquals(Scalar.of("0"), alist.get(Scalar.of("0")));
		assertEquals(Scalar.of(Integer.toString(n / 2 - 1)),
			alist.get(Scalar.of(Integer.toString(n / 2 - 1))));
	}

	static List<Map.Entry<Datum,Datum>> entriesOf(AssocList alist)
	{
		List<Map.Entry<Datum,Datum>> out = new ArrayList<>();
		Iterator<Map.Entry<Datum,Datum>> it = alist.entrySet().iterator();
		while (it.hasNext()) {
			out.add(it.next());
		}
		return out;
	}

	@Test void testEntrySetOfUniqueKeys()
	{
		assertEquals(List.of(
			Map.entry(A, Scalar.of("1")),
			Map.entry(B, Scalar.of("2")),
			Map.entry(C, Scalar.of("3"))),
			entriesOf(abc()));
	}

	/* Each key yields its first occurrence, in order of first appearance. */
	@Test void testEntrySetSkipsShadowedEntries()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, Scalar.of("1")),
			pair(B, Scalar.of("2")),
			pair(A, Scalar.of("3")),
			pair(C, Scalar.of("4")),
			pair(B, Scalar.of("5")),
			pair(A, Scalar.of("6"))));
		assertEquals(List.of(
			Map.entry(A, Scalar.of("1")),
			Map.entry(B, Scalar.of("2")),
			Map.entry(C, Scalar.of("4"))),
			entriesOf(alist));
	}

	@Test void testEntrySetOfAllDuplicates()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, B), pair(A, C), pair(A, Z)));
		assertEquals(List.of(Map.entry(A, B)), entriesOf(alist));
	}

	@Test void testEntrySetOfEmptyList()
	{
		AssocList alist = new AssocList(Nil.INSTANCE);
		assertEquals(List.of(), entriesOf(alist));
		Iterator<Map.Entry<Datum,Datum>> it = alist.entrySet().iterator();
		assertFalse(it.hasNext());
		assertThrows(NoSuchElementException.class, it::next);
	}

	/* Repeated hasNext() calls must neither skip nor consume entries. */
	@Test void testEntrySetHasNextIsIdempotent()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, B), pair(A, C), pair(B, C)));
		Iterator<Map.Entry<Datum,Datum>> it = alist.entrySet().iterator();
		for (int i = 0; i < 3; ++i) {
			assertTrue(it.hasNext());
		}
		assertEquals(Map.entry(A, B), it.next());
		assertEquals(Map.entry(B, C), it.next());
		assertFalse(it.hasNext());
		assertFalse(it.hasNext());
		assertThrows(NoSuchElementException.class, it::next);
	}
}
