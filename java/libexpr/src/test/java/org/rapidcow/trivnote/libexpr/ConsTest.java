/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API tests for ConsList and the Cons/Datum factories.
 * Verify observable structure without requiring a factory to choose a
 * particular Cons implementation. Datum's list factory is fromList().
 */
@DisplayName("Cons API tests")
class ConsTest
{
	static final Datum A = Symbol.of("a");
	static final Datum B = Symbol.of("b");
	static final Datum C = Symbol.of("c");
	static final Datum Z = Symbol.of("z");

	/* Walk car/cdr directly so list-view caches cannot hide a wrong spine. */
	static void checkSpine(Datum actual, Datum tail, Datum... elements)
	{
		for (Datum element : elements) {
			Cons cell = assertInstanceOf(Cons.class, actual);
			assertSame(element, cell.car());
			actual = cell.cdr();
		}
		assertSame(tail, actual);
	}

	static void checkList(ConsList actual, Datum tail, Datum... elements)
	{
		checkSpine(actual, tail, elements);
		assertEquals(elements.length, actual.size());
		assertSame(tail, actual.tail());
	}

	@Test void testDequePrefixOrderAndSingleCellSuffix()
	{
		for (Datum tail : List.of(Nil.INSTANCE, Z)) {
			for (List<Datum> prefix : List.of(List.<Datum>of(), List.of(A), List.of(A, B))) {
				Deque<Datum> q = new ArrayDeque<>();
				for (Datum element : prefix) {
					q.push(element);
				}
				List<Datum> elements = new ArrayList<>(prefix);
				elements.add(C);
				Datum[] expected = elements.toArray(Datum[]::new);
				Cons suffix = Cons.of(C, tail);
				checkList(new ConsList(new ArrayDeque<>(q), suffix), tail, expected);
				Cons result = Cons.of(q, suffix);
				checkSpine(result, tail, expected);
				assertTrue(q.isEmpty());
				assertEquals(expected.length, Datum.asList(result).count());
				assertSame(tail, Datum.asList(result).tail());
			}
		}
	}

	@Test void testDequePrefixWithLongerSuffix()
	{
		for (Datum tail : List.of(Nil.INSTANCE, Z)) {
			/* Exercise both a cached suffix and an ordinary cons chain. */
			for (Cons suffix : List.of(
				new ConsList(B, C, tail), Cons.of(B, Cons.of(C, tail)))) {
				Deque<Datum> q = new ArrayDeque<>(List.of(A));
				ConsList result = new ConsList(q, suffix);
				checkList(result, tail, A, B, C);
				assertTrue(q.isEmpty());
			}
		}
	}

	@Test void testEmptyDequePreservesSuffix()
	{
		Cons suffix = Cons.of(A, Z);
		assertSame(suffix, Cons.of(new ArrayDeque<>(), suffix));
	}

	@Test void testDequeConstructionRejectsNulls()
	{
		Cons suffix = Cons.ofList(A);
		assertThrows(NullPointerException.class, () -> Cons.of((Deque<Datum>) null, suffix));
		assertThrows(NullPointerException.class, () -> Cons.of(new ArrayDeque<>(), null));
		assertThrows(NullPointerException.class, () -> new ConsList((Deque<Datum>) null, suffix));
		assertThrows(NullPointerException.class, () -> new ConsList(new ArrayDeque<>(), null));
		Deque<Datum> q = new LinkedList<>();
		q.add(null);
		assertThrows(NullPointerException.class, () -> Cons.of(q, suffix));
		q.add(null);
		assertThrows(NullPointerException.class, () -> new ConsList(q, suffix));
	}

	@Test void testConsOfFixedArityPreservesBothHalves()
	{
		Cons nested = Cons.ofList(B, C);
		for (Datum car : List.of(A, Nil.INSTANCE, nested)) {
			for (Datum cdr : List.of(Z, Nil.INSTANCE, nested)) {
				Cons fixed = Cons.of(car, cdr);
				checkSpine(fixed, cdr, car);
				/* An explicit array selects the original varargs signature. */
				Cons varargs = Cons.of(car, cdr, new Datum[0]);
				checkSpine(varargs, cdr, car);
				assertEquals(fixed, varargs);
				assertEquals(varargs, fixed);
				assertEquals(fixed.hashCode(), varargs.hashCode());
			}
		}
	}

	@Test void testOfListFixedSingletonMatchesExplicitArray()
	{
		Cons nested = Cons.ofList(B, C);
		for (Datum first : List.of(A, Nil.INSTANCE, nested)) {
			Cons fixed = Cons.ofList(first);
			Cons varargs = Cons.ofList(first, new Datum[0]);
			checkSpine(fixed, Nil.INSTANCE, first);
			checkSpine(varargs, Nil.INSTANCE, first);
			assertEquals(fixed, varargs);
			assertEquals(varargs, fixed);
			assertEquals(fixed.hashCode(), varargs.hashCode());
			DatumList view = Datum.asProperList(fixed);
			assertEquals(1L, view.count());
			assertEquals(List.of(first), view);
		}
	}

	@Test void testOfListFixedPairMatchesExplicitArray()
	{
		Cons nested = Cons.ofList(B, C);
		for (Datum first : List.of(A, Nil.INSTANCE, nested)) {
			for (Datum second : List.of(B, Nil.INSTANCE, nested)) {
				Cons fixed = Cons.ofList(first, second);
				Cons varargs = Cons.ofList(first, new Datum[] { second });
				checkSpine(fixed, Nil.INSTANCE, first, second);
				checkSpine(varargs, Nil.INSTANCE, first, second);
				assertEquals(fixed, varargs);
				assertEquals(varargs, fixed);
				assertEquals(fixed.hashCode(), varargs.hashCode());
				DatumList view = Datum.asProperList(fixed);
				assertEquals(2L, view.count());
				assertEquals(List.of(first, second), view);
			}
		}
	}

	@Test void testConsOfExplicitShortVarargsPreservesTail()
	{
		Cons nested = Cons.ofList(B, C);
		for (Datum tail : List.of(Nil.INSTANCE, Z, nested)) {
			checkSpine(Cons.of(A, B, new Datum[] { tail }), tail, A, B);
		}
		Cons proper = Cons.of(A, B, Nil.INSTANCE);
		assertEquals(Cons.ofList(A, B), proper);
		assertEquals(List.of(A, B), Datum.asProperList(proper));
	}

	@Test void testShortFactoriesRejectNullElements()
	{
		assertThrows(NullPointerException.class, () -> Cons.of(null, Nil.INSTANCE));
		assertThrows(NullPointerException.class, () -> Cons.of(null, Z, new Datum[0]));
		assertThrows(NullPointerException.class, () -> Cons.of(A, (Datum) null, new Datum[0]));
		assertThrows(NullPointerException.class, () -> Cons.of(null, B, Nil.INSTANCE));
		assertThrows(NullPointerException.class, () -> Cons.of(A, (Datum) null, Nil.INSTANCE));
		assertThrows(NullPointerException.class, () -> Cons.of(A, B, new Datum[] { null }));
		assertThrows(NullPointerException.class, () -> Cons.ofList(null));
		assertThrows(NullPointerException.class, () -> Cons.ofList(null, B));
		assertThrows(NullPointerException.class, () -> Cons.ofList(A, (Datum) null));
		assertThrows(NullPointerException.class, () -> Cons.ofList(null, new Datum[0]));
		assertThrows(NullPointerException.class, () -> Cons.ofList(A, new Datum[] { null }));
		for (Datum[] elements : new Datum[][] { { null }, { null, B }, { A, null } }) {
			assertThrows(NullPointerException.class, () -> Cons.fromList(Arrays.asList(elements)));
		}
	}

	@Test void testShortFromSequentialListsCopiesElements()
	{
		for (List<Datum> elements : List.of(List.of(A), List.of(A, B))) {
			List<Datum> input = new LinkedList<>(elements);
			Cons result = Cons.fromList(input);
			input.clear();
			checkSpine(result, Nil.INSTANCE, elements.toArray(Datum[]::new));
			assertEquals(elements, Datum.asProperList(result));
		}
	}

	@Test void testShortFromDatumListsDiscardsImproperTail()
	{
		for (Datum tail : List.of(Nil.INSTANCE, Z)) {
			List<Datum> singleton = Datum.asList(Cons.of(A, tail));
			List<Datum> pair = Datum.asList(Cons.of(A, B, tail));
			checkSpine(Cons.fromList(singleton), Nil.INSTANCE, A);
			checkSpine(Cons.fromList(pair), Nil.INSTANCE, A, B);
		}
	}

	@Test void testConsOfVarargs()
	{
		for (Datum tail : List.of(Nil.INSTANCE, Z)) {
			checkSpine(Cons.of(A, tail), tail, A);
			checkSpine(Cons.of(A, B, tail), tail, A, B);
			checkSpine(Cons.of(A, B, C, tail), tail, A, B, C);
			checkSpine(Cons.of(A, B, C, A, tail), tail, A, B, C, A);
		}
	}

	@Test void testConsOfArrayAndTail()
	{
		for (Datum tail : List.of(Nil.INSTANCE, Z)) {
			checkSpine(Cons.of(A, new Datum[0], tail), tail, A);
			checkSpine(Cons.of(A, new Datum[] { B }, tail), tail, A, B);
			checkSpine(Cons.of(A, new Datum[] { B, C }, tail), tail, A, B, C);
		}
	}

	@Test void testConsListVarargs()
	{
		for (Datum tail : List.of(Nil.INSTANCE, Z)) {
			checkList(new ConsList(A, tail), tail, A);
			checkList(new ConsList(A, B, tail), tail, A, B);
			checkList(new ConsList(A, B, C, tail), tail, A, B, C);
			checkList(new ConsList(A, B, C, A, tail), tail, A, B, C, A);
		}
	}

	@Test void testConsListArrayAndTail()
	{
		for (Datum tail : List.of(Nil.INSTANCE, Z)) {
			checkList(new ConsList(A, new Datum[0], tail), tail, A);
			checkList(new ConsList(A, new Datum[] { B }, tail), tail, A, B);
			checkList(new ConsList(A, new Datum[] { B, C }, tail), tail, A, B, C);
		}
	}

	@Test void testConsTailIsPreserved()
	{
		Cons tail = Cons.ofList(B, C);
		checkSpine(Cons.of(A, tail), tail, A);
		checkSpine(Cons.of(A, B, tail), tail, A, B);
		checkSpine(Cons.of(A, new Datum[] { B }, tail), tail, A, B);
	}

	@Test void testOfListTreatsEveryArgumentAsAnElement()
	{
		checkSpine(Cons.ofList(A), Nil.INSTANCE, A);
		checkSpine(Cons.ofList(A, B, C), Nil.INSTANCE, A, B, C);
		Cons nested = Cons.ofList(B, C);
		checkSpine(Cons.ofList(Nil.INSTANCE, nested), Nil.INSTANCE, Nil.INSTANCE, nested);
	}

	@Test void testFromList()
	{
		for (List<Datum> input : List.of(List.of(A), List.of(A, B), List.of(A, B, C))) {
			Datum[] elements = input.toArray(Datum[]::new);
			checkList(new ConsList(input), Nil.INSTANCE, elements);
			checkSpine(Cons.fromList(input), Nil.INSTANCE, elements);
			checkSpine(Datum.fromList(input), Nil.INSTANCE, elements);
			assertEquals(input, Datum.asList(Datum.fromList(input)));
		}
	}

	@Test void testFromDatumList()
	{
		List<Datum> input = Datum.asList(Cons.ofList(A, B, C));
		checkList(new ConsList(input), Nil.INSTANCE, A, B, C);
		checkSpine(Cons.fromList(input), Nil.INSTANCE, A, B, C);
		checkSpine(Datum.fromList(input), Nil.INSTANCE, A, B, C);
	}

	/* A list view only promises its elements, never its tail */
	@Test void testFromImproperDatumList()
	{
		List<Datum> input = new DatumList(Cons.of(A, B, C, Z));
		checkList(new ConsList(input), Nil.INSTANCE, A, B, C);
		checkSpine(Cons.fromList(input), Nil.INSTANCE, A, B, C);
		checkSpine(Datum.fromList(input), Nil.INSTANCE, A, B, C);
	}

	@Test void testEmptyList()
	{
		assertSame(Nil.INSTANCE, Datum.fromList(List.of()));
		assertSame(Nil.INSTANCE, Datum.fromList(Datum.asList(Nil.INSTANCE)));
		assertThrows(IllegalArgumentException.class, () -> Cons.fromList(List.of()));
		assertThrows(IllegalArgumentException.class, () -> new ConsList(List.of()));
	}

	@Test void testInputArraysAreNotRetained()
	{
		Datum[] input = { B, C };
		List<Cons> results = List.of(
			Cons.of(A, Z, input), new ConsList(A, Z, input),
			Cons.of(A, input, Z), new ConsList(A, input, Z),
			Cons.ofList(A, input));
		Arrays.fill(input, Z);
		checkSpine(results.get(0), C, A, Z, B);
		checkSpine(results.get(1), C, A, Z, B);
		checkSpine(results.get(2), Z, A, B, C);
		checkSpine(results.get(3), Z, A, B, C);
		checkSpine(results.get(4), Nil.INSTANCE, A, B, C);
	}

	@Test void testInputListsAreNotRetained()
	{
		List<Datum> input = new ArrayList<>(List.of(A, B, C));
		ConsList direct = new ConsList(input);
		Cons cons = Cons.fromList(input);
		Datum datum = Datum.fromList(input);
		input.set(0, Z);
		input.clear();
		checkList(direct, Nil.INSTANCE, A, B, C);
		checkSpine(cons, Nil.INSTANCE, A, B, C);
		checkSpine(datum, Nil.INSTANCE, A, B, C);
	}

	@Test void testStructuralEqualityAndHashCode()
	{
		Cons cells = new ConsCell(A, new ConsCell(B, new ConsCell(C, Z)));
		ConsList list = new ConsList(A, B, C, Z);
		assertEquals(cells, list);
		assertEquals(list, cells);
		assertEquals(cells.hashCode(), list.hashCode());
		assertEquals(cells.hashCode(), list.hashCode());
		assertNotEquals(Cons.of(A, B, C, Nil.INSTANCE), list);
		assertNotEquals(Cons.of(B, B, C, Z), list);
		assertNotEquals(A, list);
	}

	/* Fresh scalars each time, so equality cannot lean on identity. */
	static Datum longList(int n, Datum tail)
	{
		Datum list = tail;
		for (int i = n - 1; i >= 0; --i) {
			list = new ConsCell(NumberScalar.of(Integer.toString(i)), list);
		}
		return list;
	}

	static Datum deepNest(int n, Datum core)
	{
		Datum nest = core;
		for (int i = 0; i < n; ++i) {
			nest = new ConsCell(nest, Nil.INSTANCE);
		}
		return nest;
	}

	@Test void testLongListEqualityAndHashCode()
	{
		Datum x = longList(200_000, Nil.INSTANCE);
		Datum y = longList(200_000, Nil.INSTANCE);
		assertEquals(x, y);
		assertEquals(x.hashCode(), y.hashCode());
		assertNotEquals(x, longList(200_000, Z));
		assertNotEquals(x, longList(199_999, Nil.INSTANCE));
	}

	@Test void testDeepNestingEqualityAndHashCode()
	{
		Datum x = deepNest(200_000, A);
		Datum y = deepNest(200_000, A);
		assertEquals(x, y);
		assertEquals(x.hashCode(), y.hashCode());
		assertNotEquals(x, deepNest(200_000, B));
	}

	@Test void testHashCodeMatchesObjectsHash()
	{
		Cons cell = new ConsCell(A, new ConsCell(B, Nil.INSTANCE));
		int inner = Objects.hash(B, Nil.INSTANCE);
		assertEquals(Objects.hash(A, inner), cell.hashCode());
	}

	@Test void testCachedHashesStillCompareCorrectly()
	{
		/* Equal trees whose hashes were both cached first */
		Datum x = longList(1000, Z);
		Datum y = longList(1000, Z);
		x.hashCode();
		y.hashCode();
		assertEquals(x, y);
		/* Unequal trees with cached hashes are told apart */
		Datum w = longList(1000, C);
		w.hashCode();
		assertNotEquals(x, w);
		assertNotEquals(w, x);
	}

	/* A lazy view: (from from+1 ... to-1), new objects on every call */
	static final class Range extends Cons
	{
		final int from, to;

		Range(int from, int to)
		{
			this.from = from;
			this.to = to;
		}

		@Override public Datum car()
		{
			return NumberScalar.of(Integer.toString(from));
		}

		@Override public Datum cdr()
		{
			return (from + 1 < to) ? new Range(from + 1, to) : Nil.INSTANCE;
		}
	}

	@Test void testHashCodeOfFreshViews()
	{
		Datum view = new Range(0, 100_000);
		Datum cells = longList(100_000, Nil.INSTANCE);
		assertEquals(cells.hashCode(), view.hashCode());
		assertEquals(cells, view);
		assertEquals(view, cells);
	}

	@Test void testNullArgumentsAreRejected()
	{
		assertThrows(NullPointerException.class, () -> Cons.of(null, Z));
		assertThrows(NullPointerException.class, () -> Cons.of(A, (Datum) null));
		assertThrows(NullPointerException.class, () -> new ConsList(null, Z));
		assertThrows(NullPointerException.class, () -> new ConsList(A, (Datum) null));
		assertThrows(NullPointerException.class, () -> Cons.of(A, B, (Datum[]) null));
		assertThrows(NullPointerException.class, () -> new ConsList(A, B, (Datum[]) null));
		assertThrows(NullPointerException.class, () -> Cons.of(A, (Datum[]) null, Z));
		assertThrows(NullPointerException.class, () -> new ConsList(A, (Datum[]) null, Z));
		assertThrows(NullPointerException.class, () -> Cons.of(null, new Datum[0], Z));
		assertThrows(NullPointerException.class, () -> new ConsList(null, new Datum[0], Z));
		assertThrows(NullPointerException.class, () -> Cons.of(A, new Datum[0], null));
		assertThrows(NullPointerException.class, () -> new ConsList(A, new Datum[0], null));
		assertThrows(NullPointerException.class, () -> Cons.ofList(null));
		assertThrows(NullPointerException.class, () -> Cons.ofList(A, (Datum[]) null));
		assertThrows(NullPointerException.class, () -> Cons.fromList(null));
		assertThrows(NullPointerException.class, () -> Datum.fromList(null));
		assertThrows(NullPointerException.class, () -> new ConsList((List<Datum>) null));
		for (int i = 0; i < 3; ++i) {
			Datum[] input = { A, B, C };
			input[i] = null;
			assertThrows(NullPointerException.class, () -> Cons.of(A, B, input));
			assertThrows(NullPointerException.class, () -> new ConsList(A, B, input));
			assertThrows(NullPointerException.class, () -> Cons.of(A, input, Z));
			assertThrows(NullPointerException.class, () -> new ConsList(A, input, Z));
			assertThrows(NullPointerException.class, () -> Cons.ofList(A, input));
			assertThrows(NullPointerException.class, () -> Cons.fromList(Arrays.asList(input)));
			assertThrows(NullPointerException.class, () -> Datum.fromList(Arrays.asList(input)));
			assertThrows(NullPointerException.class, () -> new ConsList(Arrays.asList(input)));
		}
	}
}
