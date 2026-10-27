/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NumberCompareTest
{
	private static int cmp(String a, String b)
	{
		return Integer.signum(
			NumberScalar.of(a).compareNumericTo(NumberScalar.of(b)));
	}

	@Test void testZero()
	{
		assertEquals(NumberScalar.of("0"), NumberScalar.ZERO);
		assertTrue(NumberScalar.ZERO.isZero());
		for (String s : new String[] {"0", "-0", "+0.0", "#x0", "0/5", "0e99", ".0", "0#"}) {
			assertTrue(NumberScalar.of(s).isZero(), s);
		}
		for (String s : new String[] {"1", "-1", "0.001", "+nan.0", "+inf.0"}) {
			assertFalse(NumberScalar.of(s).isZero(), s);
		}
	}

	@Test void testOfInt()
	{
		assertEquals(NumberScalar.of("256"), NumberScalar.of(256));
		assertEquals(NumberScalar.of("-7"), NumberScalar.of(-7));
		assertEquals(NumberScalar.of(Integer.toString(Integer.MIN_VALUE)),
			NumberScalar.of(Integer.MIN_VALUE));
		NumberScalar n = NumberScalar.of(-12);
		assertEquals("-", n.sign());
		assertEquals("12", n.mantissa());
		assertEquals("", n.exponent());
		assertEquals(10, n.radix());
		assertEquals(Exactness.UNSPECIFIED, n.exactness());
		assertEquals("2147483648", NumberScalar.of(Integer.MIN_VALUE).mantissa());
		assertEquals(NumberScalar.of(5).hashCode(), NumberScalar.of("5").hashCode());
	}

	@Test void testNumericIsNotSymbolic()
	{
		assertNotEquals(NumberScalar.of("1.0"), NumberScalar.of("1"));
		assertTrue(NumberScalar.of("1.0").equalsNumeric(NumberScalar.of("1")));
		assertEquals(0, cmp("#x10", "16"));
		assertEquals(0, cmp("#b101", "5"));
		assertEquals(0, cmp("#o17", "15"));
		assertEquals(0, cmp("1/2", ".5"));
		assertEquals(0, cmp("#x1/2", "0.5"));
		assertEquals(0, cmp("1e3", "1000"));
		assertEquals(0, cmp("1p3", "8"));
		assertEquals(0, cmp("#x1p-1", "0.5"));
		assertEquals(0, cmp("12#", "120"));
		assertEquals(0, cmp("1.5##", "1.5"));
		assertEquals(0, cmp("#i0.1", "1/10"));
		assertEquals(0, cmp("#x.8", "0.5"));
		assertEquals(0, cmp("#x-A.8", "-10.5"));
	}

	@Test void testOrdering()
	{
		assertEquals(-1, cmp("1/3", "0.34"));
		assertEquals(1, cmp("1/3", "0.33"));
		assertEquals(-1, cmp("-1/2", "-1/3"));
		assertEquals(-1, cmp("-5", "3"));
		assertEquals(1, cmp("10", "9.99999999999999999999999"));
		assertEquals(-1, cmp("1e-400", "1e-399"));
		assertEquals(1, cmp("1e400", "9e399"));
		assertEquals(-1, cmp("-0.0000001", "0"));
	}

	@Test void testNonFinite()
	{
		assertEquals(-1, cmp("-inf.0", "-1e400"));
		assertEquals(1, cmp("+inf.0", "1e400"));
		assertEquals(0, cmp("+inf.0", "#i+inf.0"));
		assertEquals(-1, cmp("-inf.0", "+inf.0"));
		assertEquals(1, cmp("+nan.0", "+inf.0"));
		assertEquals(0, cmp("+nan.0", "-nan.0"));
		assertTrue(NumberScalar.of("-nan.0").isNaN());
		assertTrue(NumberScalar.of("+INF.0").isInfinite());
		assertFalse(NumberScalar.of("+inf.0").isNaN());
		assertFalse(NumberScalar.of("1").isInfinite());
		assertFalse(NumberScalar.of(1).isNaN());
		assertFalse(NumberScalar.of("+nan.0").equalsNumeric(NumberScalar.of("+nan.0")));
		assertTrue(NumberScalar.of("+inf.0").equalsNumeric(NumberScalar.of("+inf.0")));
	}

	@Test void testIntAndSimpleMix()
	{
		assertEquals(0, NumberScalar.of(256).compareNumericTo(NumberScalar.of("#x100")));
		assertEquals(0, NumberScalar.of("#x100").compareNumericTo(NumberScalar.of(256)));
		assertEquals(-1, Integer.signum(NumberScalar.of(255).compareNumericTo(NumberScalar.of("255.5"))));
		assertEquals(1, Integer.signum(NumberScalar.of("255.5").compareNumericTo(NumberScalar.of(255))));
		assertEquals(-1, Integer.signum(NumberScalar.of(Integer.MIN_VALUE).compareNumericTo(NumberScalar.of(0))));
		assertTrue(NumberScalar.of(3).equalsNumeric(NumberScalar.of("3.0")));
		assertTrue(NumberScalar.of(Integer.MAX_VALUE).compareNumericTo(NumberScalar.of("+inf.0")) < 0);
	}

	/** A foreign implementation that knows nobody. */
	private static final class Opaque extends NumberScalar
	{
		private final String text;
		Opaque(String text) { this.text = text; }
		@Override public String value() { return text; }
		@Override public Exactness exactness() { return Exactness.UNSPECIFIED; }
		@Override public int radix() { return 10; }
		@Override public String sign() { return ""; }
		@Override public String mantissa() { return text; }
		@Override public String exponent() { return ""; }
	}

	@Test void testForeignSubclassFallsBack()
	{
		NumberScalar a = new Opaque("5");
		assertEquals(OptionalInt.empty(), a.tryCompareNumericTo(NumberScalar.of(5)));
		assertEquals(0, a.compareNumericTo(NumberScalar.of(5)));
		assertEquals(0, NumberScalar.of(5).compareNumericTo(a));
		assertEquals(-1, Integer.signum(a.compareNumericTo(new Opaque("6"))));
		assertEquals(1, Integer.signum(new Opaque("7.5").compareNumericTo(a)));
	}

	@Test void testPathologicalLiterals()
	{
		assertThrows(ArithmeticException.class, () -> cmp("1/0", "1"));
		assertThrows(ArithmeticException.class, () -> cmp("1e99999999999", "1"));
		assertEquals(0, cmp("0e99999999", "0"));
	}

	@Test void testPathologicalLiteralsRoundTrip() throws java.io.IOException
	{
		for (String s : new String[] {"1/0", "1e99999999999", "#x1p-99999999999"}) {
			NumberScalar n = NumberScalar.of(s);
			assertEquals(s, n.value());
			assertEquals(n, new DataLoader(new java.io.StringReader(s)).next());
			java.io.StringWriter out = new java.io.StringWriter();
			new DataDumper(out).writeDatum(n);
			assertEquals(s, out.toString());
			assertFalse(n.isNaN());
			assertFalse(n.isInfinite());
			assertThrows(ArithmeticException.class, n::isZero);
		}
	}
}
