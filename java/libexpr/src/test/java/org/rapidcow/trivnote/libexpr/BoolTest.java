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
			List.of(Bool.TRUE, Bool.TRUE, Bool.FALSE, Bool.FALSE),
			readList("(#t #true #f #false)"));
	}

	@Test void testTopLevel()
	{
		assertSame(Bool.TRUE, readAll("#t").get(0));
		assertSame(Bool.FALSE, readAll("#false").get(0));
		assertEquals(2, readAll("#t #f").size());
	}

	@Test void testTerminators()
	{
		assertIterableEquals(List.of(Bool.TRUE), readList("(#t)"));
		assertIterableEquals(List.of(Bool.TRUE), readList("(#true)"));
		assertIterableEquals(List.of(Bool.FALSE), readList("[#false]"));
		assertIterableEquals(List.of(Bool.FALSE), readList("{#f}"));
		assertIterableEquals(List.of(Bool.TRUE), readList("(#t;comment\n)"));
		assertIterableEquals(
			List.of(Bool.FALSE, Symbol.of("x")),
			readList("(#false\nx)"));
	}

	@Test void testFollowedBySpace()
	{
		assertIterableEquals(
			List.of(Bool.FALSE, Symbol.of("x")),
			readList("(#false x)"));
		assertIterableEquals(
			List.of(Bool.TRUE, Symbol.of("x")),
			readList("(#true x)"));
		assertIterableEquals(
			List.of(Bool.TRUE, Bool.FALSE),
			readList("(#t #f)"));
	}

	@Test void testFollowedByDelimiterNoSpace()
	{
		assertIterableEquals(
			List.of(Bool.TRUE, Scalar.ofString("s")),
			readList("(#t\"s\")"));
		assertIterableEquals(
			List.of(Bool.FALSE, Symbol.of("a")),
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
			List.of(Symbol.of("t"), Symbol.of("true"), Bool.TRUE),
			readList("(t true #t)"));
	}

	@Test void testCommentsStillWork()
	{
		assertIterableEquals(
			List.of(Bool.FALSE),
			readList("(#| #t |# #false)"));
		assertIterableEquals(
			List.of(Bool.TRUE),
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
