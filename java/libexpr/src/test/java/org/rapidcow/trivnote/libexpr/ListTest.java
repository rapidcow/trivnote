/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.List;
import java.util.ListIterator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
