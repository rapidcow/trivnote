/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ScalarTest
{
	@Test void testKind()
	{
		assertInstanceOf(NumberScalar.class, NumberScalar.of("1"));
		assertInstanceOf(StringScalar.class, StringScalar.of("1"));
	}

	@Test void testEqualsBySameKind()
	{
		assertEquals(NumberScalar.of("1"), NumberScalar.of("1"));
		assertEquals(StringScalar.of("1"), StringScalar.of("1"));
		assertEquals(NumberScalar.of("1").hashCode(), NumberScalar.of("1").hashCode());
		assertEquals(StringScalar.of("1").hashCode(),
			StringScalar.of("1").hashCode());
		assertNotEquals(NumberScalar.of("1"), NumberScalar.of("01"));
	}

	@Test void testStringDistinctFromBare()
	{
		Datum bare = NumberScalar.of("1");
		Datum quot = StringScalar.of("1");

		/* symmetric */
		assertNotEquals(bare, quot);
		assertNotEquals(quot, bare);

		/* both survive in a hash set */
		assertEquals(2, Set.of(bare, quot).size());
	}

	@Test void testNotEqualToSymbol()
	{
		assertNotEquals(StringScalar.of("a"), Symbol.of("a"));
		assertNotEquals(Symbol.of("a"), StringScalar.of("a"));
	}
}
