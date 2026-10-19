/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Boolean literal tests for {@link DataLoader}.
 */
class BoolTest
{
	private static List<Datum> readAll(String text)
	{
		DataLoader loader = new DataLoader(new StringReader(text));
		List<Datum> out = new ArrayList<>();
		loader.forEachRemaining(out::add);
		return out;
	}

	private static List<Datum> readList(String text)
	{
		List<Datum> out = new ArrayList<>();
		new DatumWalk(readAll(text).get(0)).forEachRemaining(out::add);
		return out;
	}

	@Test void testShortAndLong()
	{
		assertIterableEquals(
			List.of(TruthScalar.TRUE, TruthScalar.TRUE, TruthScalar.FALSE, TruthScalar.FALSE),
			readList("(#t #true #f #false)"));
	}

	@Test void testTopLevel()
	{
		assertSame(TruthScalar.TRUE, readAll("#t").get(0));
		assertSame(TruthScalar.FALSE, readAll("#false").get(0));
		assertEquals(2, readAll("#t #f").size());
	}

	@Test void testTerminators()
	{
		assertIterableEquals(List.of(TruthScalar.TRUE), readList("(#t)"));
		assertIterableEquals(List.of(TruthScalar.TRUE), readList("(#true)"));
		assertIterableEquals(List.of(TruthScalar.FALSE), readList("[#false]"));
		assertIterableEquals(List.of(TruthScalar.FALSE), readList("{#f}"));
		assertIterableEquals(List.of(TruthScalar.TRUE), readList("(#t;comment\n)"));
		assertIterableEquals(
			List.of(TruthScalar.FALSE, Symbol.of("x")),
			readList("(#false\nx)"));
	}

	@Test void testFollowedBySpace()
	{
		assertIterableEquals(
			List.of(TruthScalar.FALSE, Symbol.of("x")),
			readList("(#false x)"));
		assertIterableEquals(
			List.of(TruthScalar.TRUE, Symbol.of("x")),
			readList("(#true x)"));
		assertIterableEquals(
			List.of(TruthScalar.TRUE, TruthScalar.FALSE),
			readList("(#t #f)"));
	}

	@Test void testFollowedByDelimiterNoSpace()
	{
		assertIterableEquals(
			List.of(TruthScalar.TRUE, Scalar.ofString("s")),
			readList("(#t\"s\")"));
		assertIterableEquals(
			List.of(TruthScalar.FALSE, Symbol.of("a")),
			readList("(#false|a|)"));
	}

	@Test void testInvalid()
	{
		for (String s : new String[] {
			"#falsey", "#falsex ", "#truex", "#tr", "#fals", "#tt",
			"#ff", "#falsee", "#trueee", "#t1", "#f_",
		}) {
			assertThrows(DataLoadingException.class,
				() -> readAll(s), s);
		}
	}

	@Test void testNotConfusedWithSymbols()
	{
		assertIterableEquals(
			List.of(Symbol.of("t"), Symbol.of("true"), TruthScalar.TRUE),
			readList("(t true #t)"));
	}

	@Test void testCommentsStillWork()
	{
		assertIterableEquals(
			List.of(TruthScalar.FALSE),
			readList("(#| #t |# #false)"));
		assertIterableEquals(
			List.of(TruthScalar.TRUE),
			readList("(#;#false #t)"));
	}

	@Test void testNoIOError()
	{
		DataLoader loader = new DataLoader(new StringReader("#t"));
		loader.next();
		assertFalse(loader.hasNext());
		assertEquals(null, loader.lastIOException());
	}
}
