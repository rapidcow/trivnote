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
			List.of(
				BoolScalar.TRUE,
				BoolScalar.TRUE,
				BoolScalar.TRUE,
				BoolScalar.FALSE,
				BoolScalar.FALSE,
				BoolScalar.FALSE
			),
			readList("(#t #T #true #f #F #false)")
		);
	}

	@Test void testTopLevel()
	{
		assertSame(BoolScalar.TRUE, readAll("#t").get(0));
		assertSame(BoolScalar.FALSE, readAll("#false").get(0));
		assertEquals(2, readAll("#t #f").size());
	}

	@Test void testTerminators()
	{
		assertIterableEquals(List.of(BoolScalar.TRUE), readList("(#t)"));
		assertIterableEquals(List.of(BoolScalar.TRUE), readList("(#true)"));
		assertIterableEquals(List.of(BoolScalar.FALSE), readList("[#false]"));
		assertIterableEquals(List.of(BoolScalar.FALSE), readList("{#f}"));
		assertIterableEquals(List.of(BoolScalar.TRUE), readList("(#t;comment\n)"));
		assertIterableEquals(
			List.of(BoolScalar.FALSE, Symbol.of("x")),
			readList("(#false\nx)"));
	}

	@Test void testFollowedBySpace()
	{
		assertIterableEquals(
			List.of(BoolScalar.FALSE, Symbol.of("x")),
			readList("(#false x)"));
		assertIterableEquals(
			List.of(BoolScalar.TRUE, Symbol.of("x")),
			readList("(#true x)"));
		assertIterableEquals(
			List.of(BoolScalar.TRUE, BoolScalar.FALSE),
			readList("(#t #f)"));
	}

	@Test void testFollowedByDelimiterNoSpace()
	{
		assertIterableEquals(
			List.of(BoolScalar.TRUE, Scalar.ofString("s")),
			readList("(#t\"s\")"));
		assertIterableEquals(
			List.of(BoolScalar.FALSE, Symbol.of("a")),
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
			List.of(Symbol.of("t"), Symbol.of("true"), BoolScalar.TRUE),
			readList("(t true #t)"));
	}

	@Test void testCommentsStillWork()
	{
		assertIterableEquals(
			List.of(BoolScalar.FALSE),
			readList("(#| #t |# #false)"));
		assertIterableEquals(
			List.of(BoolScalar.TRUE),
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
