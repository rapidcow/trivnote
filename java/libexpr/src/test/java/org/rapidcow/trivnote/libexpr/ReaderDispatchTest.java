/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReaderDispatchTest
{
	private static
	class
		IEEE754Float64
	extends
		Cons
	{
		final Datum name; 
		final float data;

		IEEE754Float64(Datum a, int i32Data)
		{
			name = a;
			data = Float.intBitsToFloat(i32Data);
		}

		static IEEE754Float64 parse(Datum a, Scalar x)
		{
			int f32Data = Integer.parseInt(x.value(), 10);
			return new IEEE754Float64(a, f32Data);
		}

		@Override public Datum car()
		{
			return name;
		}

		@Override public Datum cdr()
		{
			int f32Data = Float.floatToIntBits(data);
			return Scalar.of(Integer.toString(f32Data));
		}
	}

	private static Cons
	dispatchIEEE754(Cons p)
	{
		Datum a = p.car();
		List<Datum> v = Datum.asList(p.cdr());
		Datum e = v.get(0);
		if (!(e instanceof Scalar x)) {
			return p;
		}
		return IEEE754Float64.parse(a, x);
	}

	@Test void testIEEE754Dispatch()
		throws IOException
	{
		Reader reader = new StringReader("(:ieee754 1069547520)");
		DataLoader loader = new DataLoader(reader)
			.useDispatch(
				Symbol.of(":ieee754"),
				ReaderDispatchTest::dispatchIEEE754
			);
		Datum next = loader.readDatum();
		IEEE754Float64 stuff = assertInstanceOf(
			IEEE754Float64.class, next);
		assertEquals(1.5, stuff.data);
	}
}
