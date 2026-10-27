/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
			pair(A, NumberScalar.of("1")),
			pair(B, NumberScalar.of("2")),
			pair(C, NumberScalar.of("3"))));
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
		assertThrows(NullPointerException.class, () -> new AssocList((Datum) null));
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
		assertEquals(NumberScalar.of("1"), alist.get(A));
		assertEquals(NumberScalar.of("2"), alist.get(B));
		assertEquals(NumberScalar.of("3"), alist.get(C));
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
		Datum first = StringScalar.of("first");
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, first),
			pair(B, Z),
			pair(A, StringScalar.of("second"))));
		assertSame(first, alist.get(A));
		assertSame(Z, alist.get(B));
	}

	/* Keys match by equals(), not identity, and need not be atoms. */
	@Test void testStructuralKeys()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(Cons.ofList(A, B), C),
			pair(StringScalar.of("k"), Z),
			pair(Nil.INSTANCE, A)));
		assertSame(C, alist.get(new ConsList(A, B, Nil.INSTANCE)));
		assertSame(Z, alist.get(StringScalar.of("k")));
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
			pair(StringScalar.of("k"), Z),
			pair(StringScalar.of("k"), A)));
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
			Datum key = NumberScalar.of(Integer.toString(i % (n / 2)));
			entries = new ConsCell(pair(key, NumberScalar.of(Integer.toString(i))), entries);
		}
		AssocList alist = new AssocList(entries);
		assertEquals(n / 2, alist.size());
		assertFalse(alist.hasUniqueKeys());
		/* Key 0 appears at index 0 and n/2; the first one wins. */
		assertEquals(NumberScalar.of("0"), alist.get(Scalar.ofNumber("0")));
		assertEquals(NumberScalar.of(Integer.toString(n / 2 - 1)),
			alist.get(NumberScalar.of(Integer.toString(n / 2 - 1))));
	}

	static List< Map.Entry<Datum,Datum> > entriesOf(AssocList alist)
	{
		List< Map.Entry<Datum,Datum> > out = new ArrayList<>();
		Iterator< Map.Entry<Datum,Datum> > it = alist.entrySet().iterator();
		while (it.hasNext()) {
			out.add(it.next());
		}
		return out;
	}

	@Test void testEntrySetOfUniqueKeys()
	{
		assertEquals(List.of(
			Map.entry(A, NumberScalar.of("1")),
			Map.entry(B, NumberScalar.of("2")),
			Map.entry(C, NumberScalar.of("3"))),
			entriesOf(abc()));
	}

	/* Each key yields its first occurrence, in order of first appearance. */
	@Test void testEntrySetSkipsShadowedEntries()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, NumberScalar.of("1")),
			pair(B, NumberScalar.of("2")),
			pair(A, NumberScalar.of("3")),
			pair(C, NumberScalar.of("4")),
			pair(B, NumberScalar.of("5")),
			pair(A, NumberScalar.of("6"))));
		assertEquals(List.of(
			Map.entry(A, NumberScalar.of("1")),
			Map.entry(B, NumberScalar.of("2")),
			Map.entry(C, NumberScalar.of("4"))),
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
		Iterator< Map.Entry<Datum,Datum> > it = alist.entrySet().iterator();
		assertFalse(it.hasNext());
		assertThrows(NoSuchElementException.class, it::next);
	}

	/* Repeated hasNext() calls must neither skip nor consume entries. */
	@Test void testEntrySetHasNextIsIdempotent()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, B), pair(A, C), pair(B, C)));
		Iterator< Map.Entry<Datum,Datum> > it = alist.entrySet().iterator();
		for (int i = 0; i < 3; ++i) {
			assertTrue(it.hasNext());
		}
		assertEquals(Map.entry(A, B), it.next());
		assertEquals(Map.entry(B, C), it.next());
		assertFalse(it.hasNext());
		assertFalse(it.hasNext());
		assertThrows(NoSuchElementException.class, it::next);
	}

	/* {a: 1, b: 2, c: 3}, in that iteration order */
	static Map<Datum,Datum> abcMap()
	{
		Map<Datum,Datum> map = new LinkedHashMap<>();
		map.put(A, NumberScalar.of("1"));
		map.put(B, NumberScalar.of("2"));
		map.put(C, NumberScalar.of("3"));
		return map;
	}

	@Test void testFromDatumMap()
	{
		Map<Datum,Datum> map = abcMap();
		AssocList alist = new AssocList(map);
		assertEquals(map, alist);
		assertEquals(3, alist.size());
		assertEquals(3L, alist.count());
		assertTrue(alist.hasUniqueKeys());
		/* Entries keep the map's iteration order */
		assertEquals(List.copyOf(map.entrySet()), entriesOf(alist));
		assertEquals(abc(), alist);
	}

	@Test void testFromSymbolMap()
	{
		Map<Symbol,Scalar> map = new LinkedHashMap<>();
		map.put(Symbol.of("x"), NumberScalar.of("1"));
		map.put(Symbol.of("y"), NumberScalar.of("2"));
		AssocList alist = new AssocList(map);
		assertEquals(map, alist);
		assertEquals(2, alist.size());
		assertTrue(alist.hasUniqueKeys());
		assertEquals(List.copyOf(map.entrySet()), entriesOf(alist));
		assertSame(map.get(Symbol.of("x")), alist.get(Symbol.of("x")));
	}

	@Test void testFromSingletonMap()
	{
		AssocList alist = new AssocList(Map.of(A, B));
		assertEquals(1, alist.size());
		assertSame(B, alist.get(A));
		assertEquals(List.of(Map.entry(A, B)), entriesOf(alist));
	}

	@Test void testFromEmptyMap()
	{
		AssocList alist = new AssocList(Map.of());
		assertEquals(0, alist.size());
		assertEquals(0L, alist.count());
		assertTrue(alist.hasUniqueKeys());
		assertNull(alist.get(A));
		assertEquals(List.of(), entriesOf(alist));
		assertEquals(new AssocList(Nil.INSTANCE), alist);
	}

	/* Values (and keys) may be anything, nil and structures included */
	@Test void testConstructorStructures()
	{
		Cons key = Cons.ofList(A, B);
		Cons value = Cons.of(B, C);
		Map<Datum,Datum> map = new LinkedHashMap<>();
		map.put(key, value);
		map.put(Nil.INSTANCE, Nil.INSTANCE);
		AssocList alist = new AssocList(map);
		assertSame(value, alist.get(Cons.ofList(A, B)));
		assertSame(Nil.INSTANCE, alist.get(Nil.INSTANCE));
		assertTrue(alist.containsKey(Nil.INSTANCE));
		assertEquals(2, alist.size());
	}

	/* An alist copied into an alist is the same map */
	@Test void testFromAssocList()
	{
		AssocList shadowed = new AssocList(Cons.ofList(
			pair(A, B), pair(B, C), pair(A, Z)));
		AssocList copy = new AssocList(shadowed);
		assertEquals(shadowed, copy);
		assertEquals(2, copy.size());
		assertTrue(copy.hasUniqueKeys());
		assertSame(B, copy.get(A));
	}

	@Test void testMapIsNotRetained()
	{
		Map<Datum,Datum> map = abcMap();
		AssocList alist = new AssocList(map);
		map.put(A, Z);
		map.put(Z, A);
		map.remove(B);
		assertEquals(abc(), alist);
		assertEquals(3, alist.size());
		assertNull(alist.get(Z));
	}

	/* Just like Datum.fromList, an empty map is nil */
	@Test void testDatumFromEmptyMapIsNil()
	{
		assertSame(Nil.INSTANCE, Datum.fromMap(Map.of()));
		assertSame(Nil.INSTANCE, Datum.fromMap(new AssocList(Nil.INSTANCE)));
	}

	@Test void testDatumFromMap()
	{
		Map<Datum,Datum> map = abcMap();
		Datum alist = Datum.fromMap(map);
		/* Entries keep the map's iteration order */
		assertEquals(abc().head(), alist);
		assertEquals(new AssocList(map).head(), alist);
		assertEquals(Cons.ofList(pair(A, B)), Datum.fromMap(Map.of(A, B)));
	}

	@Test void testDatumFromMapRoundTrip()
	{
		for (Map<Datum,Datum> map : List.<Map<Datum,Datum>>of(Map.of(), Map.of(A, B), abcMap())) {
			assertEquals(map, Datum.asAlist(Datum.fromMap(map)));
		}
	}

	@Test void testNullMapIsRejected()
	{
		assertThrows(NullPointerException.class,
			() -> new AssocList((Map<Datum,Datum>) null));
		assertThrows(NullPointerException.class, () -> Datum.fromMap(null));

		Map<Datum,Datum> nullKey = new HashMap<>();
		nullKey.put(null, A);
		assertThrows(NullPointerException.class, () -> new AssocList(nullKey));
		assertThrows(NullPointerException.class, () -> Datum.fromMap(nullKey));

		Map<Datum,Datum> nullValue = new HashMap<>();
		nullValue.put(A, null);
		assertThrows(NullPointerException.class, () -> new AssocList(nullValue));
		assertThrows(NullPointerException.class, () -> Datum.fromMap(nullValue));
	}

	/* Calling count() first lets withUniqueKeys() reuse the cached count */
	static AssocList counted(AssocList alist)
	{
		alist.count();
		return alist;
	}

	/* ((a . 1) (b . 2) (a . 3) (c . 4) (b . 5) (a . 6)) */
	static AssocList shadowed()
	{
		return new AssocList(Cons.ofList(
			pair(A, NumberScalar.of("1")),
			pair(B, NumberScalar.of("2")),
			pair(A, NumberScalar.of("3")),
			pair(C, NumberScalar.of("4")),
			pair(B, NumberScalar.of("5")),
			pair(A, NumberScalar.of("6"))));
	}

	static void checkUnique(AssocList alist, long n)
	{
		assertTrue(alist.hasUniqueKeys());
		assertSame(alist, alist.requireUniqueKeys());
		assertEquals(n, alist.count());
		assertEquals((int) n, alist.size());
		assertEquals(n, new DatumList(alist.head()).count());
	}

	@Test void testWithUniqueKeysDropsShadowedEntries()
	{
		for (AssocList alist : List.of(shadowed(), counted(shadowed()))) {
			AssocList uniq = alist.withUniqueKeys();
			checkUnique(uniq, 3);
			assertEquals(List.of(
				Map.entry(A, NumberScalar.of("1")),
				Map.entry(B, NumberScalar.of("2")),
				Map.entry(C, NumberScalar.of("4"))),
				entriesOf(uniq));
			assertEquals(alist, uniq);
		}
	}

	/* The surviving entries are the very same pairs, not copies */
	@Test void testWithUniqueKeysSharesPairs()
	{
		Cons a = pair(A, B);
		Cons b = pair(B, C);
		AssocList alist = new AssocList(Cons.ofList(a, pair(A, Z), b));
		ConsTest.checkSpine(alist.withUniqueKeys().head(), Nil.INSTANCE, a, b);
	}

	@Test void testWithUniqueKeysLeavesOriginalAlone()
	{
		AssocList alist = shadowed();
		Datum head = alist.head();
		alist.withUniqueKeys();
		assertSame(head, alist.head());
		assertEquals(6L, new DatumList(head).count());
		assertEquals(3, alist.size());
		assertFalse(alist.hasUniqueKeys());
	}

	@Test void testWithUniqueKeysOfStructuralKeys()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(Cons.ofList(A, B), C),
			pair(new ConsList(A, B, Nil.INSTANCE), Z),
			pair(StringScalar.of("k"), Z),
			pair(StringScalar.of("k"), A)));
		AssocList uniq = alist.withUniqueKeys();
		checkUnique(uniq, 2);
		assertSame(C, uniq.get(Cons.ofList(A, B)));
		assertSame(Z, uniq.get(StringScalar.of("k")));
	}

	@Test void testWithUniqueKeysOfAllDuplicates()
	{
		AssocList alist = new AssocList(Cons.ofList(
			pair(A, B), pair(A, C), pair(A, Z)));
		AssocList uniq = alist.withUniqueKeys();
		checkUnique(uniq, 1);
		assertEquals(List.of(Map.entry(A, B)), entriesOf(uniq));
	}

	/* Once it is known to be unique, there is nothing to do */
	@Test void testWithUniqueKeysKeepsUniqueAlist()
	{
		AssocList alist = counted(abc());
		assertSame(alist, alist.withUniqueKeys());
		AssocList empty = new AssocList(Collections.emptyMap());
		assertSame(empty, empty.withUniqueKeys());
	}

	/* Checking first is how callers ask to reuse the same alist */
	@Test void testWithUniqueKeysAfterCheckingUniqueness()
	{
		AssocList checked = abc();
		assertTrue(checked.hasUniqueKeys());
		assertSame(checked, checked.withUniqueKeys());

		AssocList required = abc().requireUniqueKeys();
		assertSame(required, required.withUniqueKeys());

		AssocList failed = shadowed();
		assertThrows(IllegalArgumentException.class, failed::requireUniqueKeys);
		AssocList uniq = failed.withUniqueKeys();
		checkUnique(uniq, 3);
		assertEquals(failed, uniq);
	}

	/* Even unchecked, an alist that turns out unique is kept as is */
	@Test void testWithUniqueKeysOfUncheckedUniqueAlist()
	{
		AssocList alist = abc();
		assertSame(alist, alist.withUniqueKeys());
		checkUnique(alist, 3);
		AssocList fromMap = new AssocList(abcMap());
		assertSame(fromMap, fromMap.withUniqueKeys());
		checkUnique(fromMap, 3);
	}

	/* The scan counts the original's keys as a side effect */
	@Test void testWithUniqueKeysCountsOriginal()
	{
		AssocList alist = shadowed();
		AssocList uniq = alist.withUniqueKeys();
		assertEquals(3L, alist.count());
		assertFalse(alist.hasUniqueKeys());
		assertSame(uniq.head(), uniq.withUniqueKeys().head());
	}

	@Test void testWithUniqueKeysOfEmptyList()
	{
		for (AssocList alist : List.of(
				new AssocList(Nil.INSTANCE),
				counted(new AssocList(Nil.INSTANCE)))) {
			AssocList uniq = alist.withUniqueKeys();
			assertSame(alist, uniq);
			checkUnique(uniq, 0);
			assertSame(Nil.INSTANCE, uniq.head());
			assertEquals(List.of(), entriesOf(uniq));
		}
	}

	@Test void testWithUniqueKeysIsIdempotent()
	{
		AssocList uniq = shadowed().withUniqueKeys();
		assertSame(uniq, uniq.withUniqueKeys());
		assertSame(uniq, uniq.withUniqueKeys().withUniqueKeys());
	}

	/* A map's keys are only unique by its own idea of equality */
	@Test void testConstructorDoesNotTrustMapUniqueness()
	{
		Map<Datum,Datum> map = new IdentityHashMap<>();
		map.put(Cons.ofList(A, B), C);
		map.put(Cons.ofList(A, B), Z);
		AssocList alist = new AssocList(map);
		assertEquals(1, alist.size());
		assertFalse(alist.hasUniqueKeys());
		AssocList uniq = alist.withUniqueKeys();
		checkUnique(uniq, 1);
		assertSame(alist.get(Cons.ofList(A, B)), uniq.get(Cons.ofList(A, B)));
	}
}
