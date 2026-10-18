/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListTest
{
	static final Datum A = Symbol.of("a");
	static final Datum B = Symbol.of("b");
	static final Datum C = Symbol.of("c");
	static final Datum Z = Symbol.of("z");

	/* (a b c) */
	static Datum proper()
	{
		return Cons.of(A, Cons.of(B, Cons.of(C, Nil.INSTANCE)));
	}

	/* (a b . z) */
	static Datum improper()
	{
		return Cons.of(A, Cons.of(B, Z));
	}

	@Test void testSize()
	{
		assertEquals(3, new DatumList(proper()).size());
		assertEquals(2, new DatumList(improper()).size());
		assertEquals(0, new DatumList(Nil.INSTANCE).size());
	}

	@Test void testTail()
	{
		assertSame(Nil.INSTANCE, new DatumList(proper()).tail());
		assertSame(Z, new DatumList(improper()).tail());
	}

	/* finish() caches the tail; size() must still be right afterwards */
	@Test void testSizeAfterFinish()
	{
		Datum head = proper();
		DatumList list = new DatumList(head);
		list.finish(new DatumWalk(head));
		assertSame(Nil.INSTANCE, list.tail());
		assertEquals(3, list.size());
	}

	@Test void testFinishForeign()
	{
		DatumList list = new DatumList(proper());
		assertThrows(IllegalArgumentException.class,
			() -> list.finish(new DatumWalk(proper())));
	}

	@Test void testRequireProperTail()
	{
		new DatumList(proper()).requireProperTail();
		assertThrows(IllegalArgumentException.class,
			() -> new DatumList(improper()).requireProperTail());
	}

	@Test void testGet()
	{
		DatumList list = new DatumList(proper());
		assertEquals(A, list.get(0));
		assertEquals(B, list.get(1));
		assertEquals(C, list.get(2));
		assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
		assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
	}

	@Test void testListIteratorIndex()
	{
		DatumList list = new DatumList(proper());
		ListIterator<Datum> iter = list.listIterator(3);
		assertEquals(3, iter.nextIndex());
		assertEquals(false, iter.hasNext());
		assertEquals(C, iter.previous());

		iter = list.listIterator(1);
		assertEquals(1, iter.nextIndex());
		assertEquals(B, iter.next());
	}

	/* List.listIterator(int) says IndexOutOfBoundsException */
	@Test void testListIteratorOutOfRange()
	{
		DatumList list = new DatumList(proper());
		assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(4));
		assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(-1));
	}

	@Test void testEquals()
	{
		assertEquals(List.of(A, B, C), new DatumList(proper()));
		assertEquals(List.of(A, B), new DatumList(improper()));
	}

	@Test void testToArray()
	{
		assertEquals(3, new DatumList(proper()).toArray().length);
	}

	@Test void testToProperListKeepsProper()
	{
		DatumList list = new DatumList(proper());
		assertSame(list, list.toProperList());
		DatumList empty = new DatumList(Nil.INSTANCE);
		assertSame(empty, empty.toProperList());
	}

	@Test void testToProperListRebuildsImproper()
	{
		Datum head = improper();
		DatumList proper = new DatumList(head).toProperList();
		assertSame(Nil.INSTANCE, proper.tail());
		assertEquals(List.of(A, B), proper);
		/* Walk the chain itself, not the cached tail */
		DatumWalk walk = new DatumWalk(proper.head());
		assertSame(A, walk.next());
		assertSame(B, walk.next());
		assertSame(Nil.INSTANCE, walk.rest());
		/* Idempotent once proper */
		assertSame(proper, proper.toProperList());
		/* The original is left alone */
		assertSame(Z, Datum.cdr(Datum.cdr(head)));
	}

	@Test void testToProperListOfBareAtom()
	{
		DatumList proper = new DatumList(Z).toProperList();
		assertSame(Nil.INSTANCE, proper.head());
		assertEquals(0, proper.size());
	}

	@Test void testFromDatumJavaList()
	{
		Cons nested = Cons.ofList(B, C);
		List<Datum> input = List.of(A, Scalar.of("1"), nested, Nil.INSTANCE);
		DatumList list = new DatumList(input);
		assertEquals(input, list);
		assertEquals(4, list.size());
		assertEquals(4L, list.count());
		assertSame(Nil.INSTANCE, list.tail());
		ConsTest.checkSpine(list.head(), Nil.INSTANCE,
			A, input.get(1), nested, Nil.INSTANCE);
	}

	@Test void testFromSymbolJavaList()
	{
		List<Symbol> input = List.of(Symbol.of("x"), Symbol.of("y"), Symbol.of("z"));
		DatumList list = new DatumList(input);
		assertEquals(input, list);
		assertEquals(3, list.size());
		assertSame(Nil.INSTANCE, list.tail());
		ConsTest.checkSpine(list.head(), Nil.INSTANCE, input.toArray(Datum[]::new));
	}

	@Test void testFromSingletonJavaList()
	{
		DatumList list = new DatumList(List.of(A));
		assertEquals(List.of(A), list);
		assertSame(Nil.INSTANCE, list.tail());
		ConsTest.checkSpine(list.head(), Nil.INSTANCE, A);
	}

	/* Just like Datum.fromList, an empty list is nil */
	@Test void testFromEmptyJavaList()
	{
		DatumList list = new DatumList(List.<Datum>of());
		assertSame(Nil.INSTANCE, list.head());
		assertSame(Nil.INSTANCE, list.tail());
		assertEquals(0, list.size());
		assertEquals(List.of(), list);
	}

	/* Another DatumList's tail is not copied, only its elements */
	@Test void testFromImproperDatumList()
	{
		DatumList list = new DatumList(new DatumList(improper()));
		assertEquals(List.of(A, B), list);
		assertSame(Nil.INSTANCE, list.tail());
		assertInstanceOf(Cons.class, list.head());
	}

	@Test void testJavaListIsNotRetained()
	{
		List<Datum> input = new ArrayList<>(List.of(A, B, C));
		DatumList list = new DatumList(input);
		input.set(0, Z);
		input.clear();
		assertEquals(List.of(A, B, C), list);
		ConsTest.checkSpine(list.head(), Nil.INSTANCE, A, B, C);
	}

	@Test void testNullJavaListIsRejected()
	{
		assertThrows(NullPointerException.class,
			() -> new DatumList((List<Datum>) null));
		assertThrows(NullPointerException.class,
			() -> new DatumList(Arrays.asList(A, null, C)));
	}
}
