/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ScalarTest
{
	@Test void testIsString()
	{
		assertEquals(false, NumberScalar.of("1").isString());
		assertEquals(true, StringScalar.of("1").isString());
	}

	@Test void testEqualsBySameKind()
	{
		assertEquals(NumberScalar.of("1"), Scalar.ofNumber("1"));
		assertEquals(StringScalar.of("1"), Scalar.ofString("1"));
		assertEquals(NumberScalar.of("1").hashCode(), Scalar.ofNumber("1").hashCode());
		assertEquals(StringScalar.of("1").hashCode(),
			StringScalar.of("1").hashCode());
		assertNotEquals(NumberScalar.of("1"), Scalar.ofNumber("01"));
	}

	@Test void testStringDistinctFromBare()
	{
		Scalar bare = NumberScalar.of("1");
		Scalar quot = StringScalar.of("1");

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
