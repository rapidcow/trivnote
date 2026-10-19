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
		assertEquals(false, Scalar.ofNumber("1").isString());
		assertEquals(true, Scalar.ofString("1").isString());
	}

	@Test void testEqualsBySameKind()
	{
		assertEquals(Scalar.ofNumber("1"), Scalar.ofNumber("1"));
		assertEquals(Scalar.ofString("1"), Scalar.ofString("1"));
		assertEquals(Scalar.ofNumber("1").hashCode(), Scalar.ofNumber("1").hashCode());
		assertEquals(Scalar.ofString("1").hashCode(),
			Scalar.ofString("1").hashCode());
		assertNotEquals(Scalar.ofNumber("1"), Scalar.ofNumber("01"));
	}

	@Test void testStringDistinctFromBare()
	{
		Scalar bare = Scalar.ofNumber("1");
		Scalar quot = Scalar.ofString("1");

		/* symmetric */
		assertNotEquals(bare, quot);
		assertNotEquals(quot, bare);

		/* both survive in a hash set */
		assertEquals(2, Set.of(bare, quot).size());
	}

	@Test void testNotEqualToSymbol()
	{
		assertNotEquals(Scalar.ofString("a"), Symbol.of("a"));
		assertNotEquals(Symbol.of("a"), Scalar.ofString("a"));
	}
}
