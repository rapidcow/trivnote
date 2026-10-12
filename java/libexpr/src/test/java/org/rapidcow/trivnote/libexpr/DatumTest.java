/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatumTest
{
	@Test void lessUselessTest()
	{
		Datum car;
		Datum cdr;
		Cons list;

		Walk here;
		int runit;

		/*
		 * N.B. JUnit puts "expect" before "actual"
		 */
		assertEquals(true, (car = new Scalar("hi")).isAtom());
		assertEquals(true, (cdr = Nil.INSTANCE).isAtom());
		assertEquals(false, (list = new Cons(car, cdr)).isAtom());

		assertEquals("Scalar[value=hi]", car.toString());

		here = new Walk(list);
		assertEquals(true, here.hasNext());
		assertEquals(car, here.next());
		assertEquals(false, here.hasNext());
		assertEquals(cdr, here.tail());

		runit = 0;
		for (Datum fat : list) {
			assertEquals(car, fat);
			runit++;
		}
		assertEquals(1, runit);
	}
}
