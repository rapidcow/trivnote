/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatumTest
{
	@Test void lessUselessTest()
	{
		Datum car;
		Datum cdr;
		Cons list;

		DatumWalk here;
		int runit;

		/*
		 * N.B. JUnit puts "expect" before "actual"
		 */
		assertEquals(true, Datum.isAtom(car = NumberScalar.of("7273")));
		assertEquals(true, Datum.isAtom(cdr = Nil.INSTANCE));
		assertEquals(false, Datum.isAtom(list = Cons.of(car, cdr)));

		assertEquals("Number[value=7273]", car.toString());

		here = new DatumWalk(list);
		assertEquals(true, here.hasNext());
		assertEquals(car, here.next());
		assertEquals(false, here.hasNext());
		assertEquals(cdr, here.rest());

		runit = 0;
		for (Datum fat : Datum.asList(list)) {
			assertEquals(car, fat);
			runit++;
		}
		assertEquals(1, runit);
	}

	@Test void testOfDequeEmptyIsNil()
	{
		Deque<Datum> q = new ArrayDeque<>();
		assertSame(Nil.INSTANCE, Datum.ofDeque(q));
		assertTrue(q.isEmpty());
	}

	@Test void testOfDequeSingleElement()
	{
		Datum a = NumberScalar.of("1");
		Deque<Datum> q = new ArrayDeque<>(List.of(a));

		Cons list = assertInstanceOf(Cons.class, Datum.ofDeque(q));
		assertSame(a, list.car());
		assertSame(Nil.INSTANCE, list.cdr());
		assertTrue(q.isEmpty());
	}

	@Test void testOfDequeReversesIterationOrder()
	{
		Datum a = NumberScalar.of("1");
		Datum b = NumberScalar.of("2");
		Datum c = NumberScalar.of("3");
		Deque<Datum> q = new ArrayDeque<>(List.of(a, b, c));

		Datum[] expected = {c, b, a};
		Datum rest = Datum.ofDeque(q);
		for (Datum element : expected) {
			Cons cell = assertInstanceOf(Cons.class, rest);
			assertSame(element, cell.car());
			rest = cell.cdr();
		}
		assertSame(Nil.INSTANCE, rest);
		assertTrue(q.isEmpty());
	}

	@Test void testOfDequeRejectsNullDeque()
	{
		assertThrows(NullPointerException.class, () -> Datum.ofDeque(null));
	}
}
