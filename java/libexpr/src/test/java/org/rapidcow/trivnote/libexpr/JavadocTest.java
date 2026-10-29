/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.IOException;
import java.io.Reader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Executable examples from the public API Javadocs.
 */
class JavadocTest
{
	@Test void testDataLoaderListExample() throws IOException
	{
		try (Reader input = new java.io.StringReader("(name \"Ada\")")) {
			DataLoader loader = new DataLoader(input);
			Datum datum = loader.readDatum();
			java.util.List<Datum> array = Datum.asProperList(datum);
			assertEquals(Symbol.of("name"), array.get(0));
			assertEquals(Scalar.ofString("Ada"), array.get(1));
		}
	}

	@Test void testDataLoaderAlistExample() throws IOException
	{
		try (Reader input = new java.io.StringReader("([name . \"Ada\"])")) {
			DataLoader loader = new DataLoader(input);
			Datum datum = loader.readDatum();
			java.util.Map<Datum,Datum> hash = Datum.asAlist(datum);
			assertEquals(Scalar.ofString("Ada"), hash.get(Symbol.of("name")));
			assertNull(hash.get(Symbol.of("NAME")));
		}
	}
}
