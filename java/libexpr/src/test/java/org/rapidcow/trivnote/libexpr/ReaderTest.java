/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link org.rapidcow.trivnote.libexpr.DataLoader} test.
 */
class ReaderTest
{
	private Reader input;
	private Datum token;
	private DataLoader loader;
	private List<Datum> list;
	private DatumWalk walk;

	@Test void testReadSymbol()
	{
		input = new StringReader("(foo bar baz)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("foo"),
				Symbol.of("bar"),
				Symbol.of("baz")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testReadScalar()
	{
		input = new StringReader("(2.719 -0.898 .67 -.1 \"1024\" \"abc\")");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Scalar.of("2.719"),
				Scalar.of("-0.898"),
				Scalar.of(".67"),
				Scalar.of("-.1"),
				Scalar.ofString("1024"),
				Scalar.ofString("abc")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testLineComment()
	{
		input = new StringReader("(abc;comment\ndef)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("abc"),
				Symbol.of("def")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDatumComment()
	{
		input = new StringReader("(#;A #;(OA deg 30) 42)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Scalar.of("42")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDatumCommentComment()
	{
		input = new StringReader("(display #; #;\"a\" \"b\" \"c\")");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("display"),
				Scalar.ofString("c")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPair()
	{
		input = new StringReader("(a b . c)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b")
			), list);
		assertEquals(Symbol.of("c"), walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairNested()
	{
		/* (a . (b c)) is equivalent to (a b c) */
		input = new StringReader("(a . (b c))");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of("c")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairNoSpaceBeforeBracket()
	{
		/*
		 * Dot need not be space-separated from a bracket: `.(c)'
		 * is `. (c)', and consing b onto (c) yields (b c), so the
		 * whole thing flattens to the proper list (a b c).
		 */
		input = new StringReader("(a b .(c))");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of("c")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDotGluedToAtomIsOrdinarySymbol()
	{
		/*
		 * A `.' immediately followed by a non-delimiter character
		 * is not the pair marker -- it is just the first character
		 * of a symbol.
		 */
		input = new StringReader("(a b .c)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of(".c")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDotAsOrdinarySymbolChar()
	{
		/* A dot with friends is just an ordinary symbol. */
		input = new StringReader("(... a.b)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("..."),
				Symbol.of("a.b")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	/*
	 * In R7RS there is a grammar rule specifically named
	 * "peculiar identifiers" -- this tests those.
	 *
	 * (It is a mere coincidence some of these symbols
	 * resemble faces.)
	 */
	@Test void testWeirdIdentifiers()
	{
		input = new StringReader("(+ - +. -. -.o +.=)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("+"),
				Symbol.of("-"),
				Symbol.of("+."),
				Symbol.of("-."),
				Symbol.of("-.o"),
				Symbol.of("+.=")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairLeadingDotFails()
	{
		input = new StringReader("(. a)");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairDoubleDotFails()
	{
		input = new StringReader("(a . . b)");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairExcessDatumFails()
	{
		input = new StringReader("(a . b c)");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairMissingDatumFails()
	{
		input = new StringReader("(a . )");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.lastIOException());
	}

	@Test void testTopLevelDotIsIllegal()
	{
		input = new StringReader(". a");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.lastIOException());
	}

	@Test void testTopLevelDotAtEofIsIllegal()
	{
		/* used to read as the symbol `.' */
		input = new StringReader(".");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairDotAtEofFails()
	{
		input = new StringReader("(a .");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairNoSpaceBeforeQuote()
	{
		/* `.' followed by `"' is the pair marker, as with a bracket */
		input = new StringReader("(a .\"b\")");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(List.of(Symbol.of("a")), list);
		assertEquals(Scalar.ofString("b"), walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairNoSpaceBeforeBar()
	{
		input = new StringReader("(a .|b c|)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(List.of(Symbol.of("a")), list);
		assertEquals(Symbol.of("b c"), walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testDottedPairDotBeforeLineComment()
	{
		input = new StringReader("(a .;comment\nb)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(List.of(Symbol.of("a")), list);
		assertEquals(Symbol.of("b"), walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testSymbolQuote()
	{
		input = new StringReader("(|foo bar| baz al|ter|na|tion|)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("foo bar"),
				Symbol.of("baz"),
				Symbol.of("alternation")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	@Test void testStringQuote()
	{
		/* ("say \"hi\"" "back\\slash" "line1\nline2") */
		input = new StringReader(
			"(\"say \\\"hi\\\"\" \"back\\\\slash\" \"line1\\nline2\")");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new DatumWalk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Scalar.ofString("say \"hi\""),
				Scalar.ofString("back\\slash"),
				Scalar.ofString("line1\nline2")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	private void assertStringEscape(String source, String expected)
	{
		DataLoader reader = new DataLoader(new StringReader("\"" + source + "\""));
		assertEquals(Scalar.ofString(expected), reader.next(), source);
		assertNull(reader.lastIOException());
		assertEquals(false, reader.hasNext());
	}

	@Test void testStringNamedEscapes()
	{
		assertStringEscape("\\a\\b\\t\\n\\v\\f\\r\\e\\\"\\\\\\|", "\007\b\t\n\013\014\r\033\"\\|");
		assertStringEscape("\\q", "q");
	}

	@Test void testStringOctalEscapes()
	{
		assertStringEscape("\\0z\\7z\\10z\\101\\7778\\1234", "\000z\007z\bzA\u01ff8S4");
		assertStringEscape("\\1\\12\\123", "\001\nS");
		assertStringEscape("\\7", "\007");
	}

	@Test void testStringHexEscapes()
	{
		assertStringEscape("\\xg\\xAz\\x41F\\xff\\x", "\000g\nzAF\u00ff\000");
		assertStringEscape("\\x41;", "A;");
		assertStringEscape("\\x\\n", "\000\n");
	}

	@Test void testStringBracedEscapes()
	{
		assertStringEscape("\\x{}\\o{}\\x{000041}\\o{101}", "\000\000AA");
		assertStringEscape("\\x{1F600}\\o{373000}", new String(Character.toChars(0x1f600)).repeat(2));
		assertStringEscape("\\x{10ffff}\\o{4177777}", new String(Character.toChars(0x10ffff)).repeat(2));
		assertStringEscape("\\x{000000000000000000000000000041}", "A");
	}

	@Test void testStringControlEscapes()
	{
		assertStringEscape("\\c@\\cA\\cZ\\c[\\c?\\ca", "\000\001\032\033\177!");
		assertStringEscape("\\c\"", "b");
		assertStringEscape("\\c\\", "\034");
	}

	@Test void testStringLineContinuations()
	{
		for (String ending : List.of("\n", "\r", "\r\n")) {
			assertStringEscape("a\\ \t" + ending + "\t b", "ab");
			assertStringEscape("a\\" + ending + "b", "ab");
			assertStringEscape("a\\" + ending + "  ", "a");
			assertStringEscape("a\\" + ending + (ending.equals("\r") ? "\n\nb" : "\nb"), "a\nb");
		}
		assertStringEscape("a\nb", "a\nb");
	}

	@Test void testStringInvalidEscapes()
	{
		for (String source : List.of(
			"\\x{g}", "\\o{8}", "\\o1", "\\x{41", "\\o{101",
			"\\x{80000000}", "\\o{20000000000}", "\\x{7fffffff}",
			"\\o{17777777777}", "\\x{110000}", "\\o{4200000}",
			"\\c\u0080", "\\ space")
		) {
			DataLoader reader = new DataLoader(new StringReader("\"" + source + "\""));
			assertThrows(DataLoadingException.class, reader::next, source);
		}
		for (String source : List.of("\"abc", "\"abc\\", "\"\\c", "\"\\x", "\"\\1", "\"\\ \n")) {
			DataLoader reader = new DataLoader(new StringReader(source));
			assertThrows(DataLoadingException.class, reader::next, source);
		}
	}

	@Test void testMaxDepth()
	{
		loader = new DataLoader(new StringReader("a (a) ((a)) [{(a)}]"))
			.useMaxDepth(2);
		assertEquals(2, loader.getMaxDepth());
		assertSame(Symbol.class, loader.next().getClass());
		loader.next();
		loader.next();
		assertThrows(DataLoadingException.class, loader::next);

		/* Zero allows atoms only, including the empty list */
		loader = new DataLoader(new StringReader("a ()")).useMaxDepth(0);
		loader.next();
		assertThrows(DataLoadingException.class, loader::next);

		/* Datum comments count too */
		loader = new DataLoader(new StringReader("(#;((a)) b)"))
			.useMaxDepth(2);
		assertThrows(DataLoadingException.class, loader::next);

		/* Deep input is refused instead of overflowing the stack */
		String deep = "(".repeat(1001) + ")".repeat(1001);
		loader = new DataLoader(new StringReader(deep)).useMaxDepth(1000);
		assertThrows(DataLoadingException.class, loader::next);

		assertThrows(IllegalArgumentException.class,
			() -> new DataLoader(new StringReader("")).useMaxDepth(-1));
	}

	@Test void testMaxLength()
	{
		loader = new DataLoader(new StringReader("(a b) (a b . c) (a b c)"))
			.useMaxLength(2);
		assertEquals(2, loader.getMaxLength());
		assertEquals(2, Datum.asList(loader.next()).size());
		/* The dotted tail is not an element */
		assertEquals(2, new DatumList(loader.next()).size());
		assertThrows(DataLoadingException.class, loader::next);

		/* Applies to nested lists one at a time */
		loader = new DataLoader(new StringReader("((a b) (c d))"))
			.useMaxLength(2);
		loader.next();

		/* Zero allows only the empty list */
		loader = new DataLoader(new StringReader("() (a)")).useMaxLength(0);
		assertSame(Nil.INSTANCE, loader.next());
		assertThrows(DataLoadingException.class, loader::next);

		assertThrows(IllegalArgumentException.class,
			() -> new DataLoader(new StringReader("")).useMaxLength(-1));
	}

	/*
	 * The empty list is the only instance where
	 * makeList() is called with an empty queue.
	 */
	@Test void testEmptyList()
	{
		loader = new DataLoader(new StringReader("()"));
		token = loader.next();
		assertSame(Nil.INSTANCE, token);
		assertNull(loader.lastIOException());

		loader = new DataLoader(new StringReader("[]"));
		token = loader.next();
		assertSame(Nil.INSTANCE, token);
		assertNull(loader.lastIOException());
	}
}
