/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
		assertEquals(true, Datum.isAtom(car = Scalar.ofNumber("7273")));
		assertEquals(true, Datum.isAtom(cdr = Nil.INSTANCE));
		assertEquals(false, Datum.isAtom(list = Cons.of(car, cdr)));

		assertEquals("Scalar[value=7273]", car.toString());

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
}
