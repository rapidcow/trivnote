/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NumberParserTest
{
	@Test void testPrefixesPreserveSpellingAndMetadata()
	{
		String[] radices = {"", "#b", "#O", "#d", "#X"};
		int[] bases = {10, 2, 8, 10, 16};
		String[] exactness = {"", "#E", "#i"};
		Exactness[] modes = {Exactness.UNSPECIFIED, Exactness.EXACT, Exactness.LOSSY};
		for (int r = 0; r < radices.length; r++) {
			for (int e = 0; e < exactness.length; e++) {
				for (String prefix : new String[] {
					radices[r] + exactness[e], exactness[e] + radices[r]}) {
					String token = prefix + "-01";
					NumberScalar n = Scalar.ofNumber(token);
					assertEquals(token, n.value());
					assertEquals(prefix, n.prefix());
					assertEquals("-01", n.number());
					assertEquals(bases[r], n.radix());
					assertEquals(modes[e], n.exactness());
					assertEquals(n, new DataLoader(new StringReader(token)).next());
				}
			}
		}
	}

	@Test void testCompleteComponents()
	{
		String[][] cases = {
			{"5/12", "", "5/12", ""},
			{"#x5/12", "", "5/12", ""},
			{"#E#X-1.8P+00", "-", "1.8", "P+00"},
			{"10.5e3", "", "10.5", "e3"},
			{"#x1e3", "", "1e3", ""},
			{"-.5", "-", ".5", ""},
			{"1.2##", "", "1.2##", ""},
			{"#b10p-12", "", "10", "p-12"}
		};
		for (String[] c : cases) {
			NumberScalar n = Scalar.ofNumber(c[0]);
			assertEquals(c[0], n.value());
			assertEquals(c[1], n.sign());
			assertEquals(c[2], n.mantissa());
			assertEquals(c[3], n.exponent());
		}
	}

	@Test void testRejectMalformedTokens()
	{
		for (String token : new String[] {
			"", "#", "#x", "#e", "#q1", "#e#i1", "#x#d1",
			"#b2", "#o8", "abc1", "1abc", "1/", "1/2junk",
			"1e", "1e+", "1.2.3", " 1", "1 ", "+nan.0junk",
			"+\u0131nf.0", "+\u0130nf.0", "-nan.0\u0130"}) {
			assertThrows(IllegalArgumentException.class, () -> Scalar.ofNumber(token), token);
		}
	}

	@Test void testSpecialValuesAndSignedZero()
	{
		for (String token : new String[] {
			"+nan.0", "-nan.0", "+inf.0", "-inf.0",
			"#i+NaN.0", "#D#I-INF.0", "+0.0", "-0.0"}) {
			NumberScalar n = (NumberScalar) Scalar.ofNumber(token);
			assertEquals(token, n.value());
			assertEquals("", n.exponent());
		}
		assertNotEquals(Scalar.ofNumber("+0.0"), Scalar.ofNumber("-0.0"));
		assertNotEquals(Scalar.ofNumber("#xA"), Scalar.ofNumber("#Xa"));
		assertNotEquals(Scalar.ofNumber("#e#x1"), Scalar.ofNumber("#x#e1"));
	}
}
