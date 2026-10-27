/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
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
				NumberScalar.of("2.719"),
				NumberScalar.of("-0.898"),
				NumberScalar.of(".67"),
				NumberScalar.of("-.1"),
				StringScalar.of("1024"),
				StringScalar.of("abc")
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
				NumberScalar.of("42")
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
				StringScalar.of("c")
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
		assertEquals(StringScalar.of("b"), walk.rest());
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

	@DisplayName("Test IEEE-754 inf/nan")
	@Test void testIEEE754FloatLiterals()
	{
		/* IEEE-754 float literals are *almost* symbols,
		 * if not for their un-|...|'d nature.  See if
		 * we can distinguish them. */
		input = new StringReader("(+inf.0 +|inf.0| +inf)");
		loader = new DataLoader(input);
		token = loader.next();
		list = Datum.asList(token);
		assertIterableEquals(
			List.of(
				NumberScalar.of("+inf.0"),
				Symbol.of("+inf.0"),
				Symbol.of("+inf")
			), list
		);
	}

	@Test void testSpecialNumbersAndQuotedSymbols()
	{
		/* More brutal tests contributed by GPT-6 Astra */
		for (String literal : List.of("+nan.0", "-nan.0", "+inf.0", "-inf.0")) {
			/* The sentinel checks that the whole token was consumed. */
			DataLoader bare = new DataLoader(new StringReader(literal + " sentinel"));
			assertEquals(NumberScalar.of(literal), bare.next());
			assertEquals(Symbol.of("sentinel"), bare.next());
			assertEquals(false, bare.hasNext());

			String[][] quoted = {
				{"|" + literal + "|", literal},
				{literal.substring(0, 1) + "|" + literal.substring(1) + "|", literal},
				{literal.substring(0, 4) + "|" + literal.substring(4) + "|", literal},
				{literal + "||", literal},
				{literal + "|suffix|", literal + "suffix"},
				{literal + "suffix", literal + "suffix"}
			};
			for (String[] c : quoted) {
				DataLoader reader = new DataLoader(new StringReader(c[0] + " sentinel"));
				assertEquals(Symbol.of(c[1]), reader.next(), c[0]);
				assertEquals(Symbol.of("sentinel"), reader.next(), c[0]);
				assertEquals(false, reader.hasNext(), c[0]);
			}
		}
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
				StringScalar.of("say \"hi\""),
				StringScalar.of("back\\slash"),
				StringScalar.of("line1\nline2")
			), list);
		assertEquals(Nil.INSTANCE, walk.rest());
		assertNull(loader.lastIOException());
	}

	private void checkStringEscape(String source, String expected)
	{
		DataLoader reader = new DataLoader(new StringReader("\"" + source + "\""));
		assertEquals(StringScalar.of(expected), reader.next(), source);
		assertNull(reader.lastIOException());
		assertEquals(false, reader.hasNext());
	}

	@Test void testStringNamedEscapes()
	{
		checkStringEscape("\\a\\b\\t\\n\\v\\f\\r\\e\\\"\\\\\\|", "\007\b\t\n\013\014\r\033\"\\|");
		checkStringEscape("\\q", "q");
	}

	@Test void testStringOctalEscapes()
	{
		checkStringEscape("\\0z\\7z\\10z\\101\\7778\\1234", "\000z\007z\bzA\u01ff8S4");
		checkStringEscape("\\1\\12\\123", "\001\nS");
		checkStringEscape("\\7", "\007");
	}

	@Test void testStringHexEscapes()
	{
		checkStringEscape("\\xg\\xAz\\x41F\\xff\\x", "\000g\nzAF\u00ff\000");
		checkStringEscape("\\x41;", "A;");
		checkStringEscape("\\x\\n", "\000\n");
	}

	@Test void testStringBracedEscapes()
	{
		checkStringEscape("\\x{}\\o{}\\x{000041}\\o{101}", "\000\000AA");
		checkStringEscape("\\x{1F600}\\o{373000}", new String(Character.toChars(0x1f600)).repeat(2));
		checkStringEscape("\\x{10ffff}\\o{4177777}", new String(Character.toChars(0x10ffff)).repeat(2));
		checkStringEscape("\\x{000000000000000000000000000041}", "A");
	}

	@Test void testStringControlEscapes()
	{
		checkStringEscape("\\c@\\cA\\cZ\\c[\\c?\\ca", "\000\001\032\033\177!");
		checkStringEscape("\\c\"", "b");
		checkStringEscape("\\c\\", "\034");
	}

	@Test void testStringLineContinuations()
	{
		for (String ending : List.of("\n", "\r", "\r\n")) {
			checkStringEscape("a\\ \t" + ending + "\t b", "ab");
			checkStringEscape("a\\" + ending + "b", "ab");
			checkStringEscape("a\\" + ending + "  ", "a");
			checkStringEscape("a\\" + ending + (ending.equals("\r") ? "\n\nb" : "\nb"), "a\nb");
		}
		checkStringEscape("a\nb", "a\nb");
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

	@Test void testDepthLimit()
	{
		loader = new DataLoader(new StringReader("a (a) ((a)) [{(a)}]"))
			.useDepthLimit(2);
		assertEquals(2, loader.getDepthLimit());
		assertSame(Symbol.class, loader.next().getClass());
		loader.next();
		loader.next();
		assertThrows(DataLoadingException.class, loader::next);

		/* Zero allows atoms only, including the empty list */
		loader = new DataLoader(new StringReader("a ()")).useDepthLimit(0);
		loader.next();
		assertThrows(DataLoadingException.class, loader::next);

		/* Datum comments count too */
		loader = new DataLoader(new StringReader("(#;((a)) b)"))
			.useDepthLimit(2);
		assertThrows(DataLoadingException.class, loader::next);

		/* Deep input is refused instead of overflowing the stack */
		String deep = "(".repeat(1001) + ")".repeat(1001);
		loader = new DataLoader(new StringReader(deep)).useDepthLimit(1000);
		assertThrows(DataLoadingException.class, loader::next);

		assertThrows(IllegalArgumentException.class,
			() -> new DataLoader(new StringReader("")).useDepthLimit(-1));
	}

	@Test void testLengthLimit()
	{
		loader = new DataLoader(new StringReader("(a b) (a b . c) (a b c)"))
			.useLengthLimit(2);
		assertEquals(2, loader.getLengthLimit());
		assertEquals(2, Datum.asList(loader.next()).size());
		/* The dotted tail is not an element */
		assertEquals(2, new DatumList(loader.next()).size());
		assertThrows(DataLoadingException.class, loader::next);

		/* Applies to nested lists one at a time */
		loader = new DataLoader(new StringReader("((a b) (c d))"))
			.useLengthLimit(2);
		loader.next();

		/* Zero allows only the empty list */
		loader = new DataLoader(new StringReader("() (a)")).useLengthLimit(0);
		assertSame(Nil.INSTANCE, loader.next());
		assertThrows(DataLoadingException.class, loader::next);

		assertThrows(IllegalArgumentException.class,
			() -> new DataLoader(new StringReader("")).useLengthLimit(-1));
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

	private void checkSymbolEscape(String source, String expected)
	{
		DataLoader reader = new DataLoader(new StringReader("|" + source + "|"));
		assertEquals(Symbol.of(expected), reader.next(), source);
		assertNull(reader.lastIOException());
		assertEquals(false, reader.hasNext());
	}

	/* Inside |...| the escapes are the same as inside "...". */
	@Test void testSymbolNamedEscapes()
	{
		checkSymbolEscape("\\a\\b\\t\\n\\v\\f\\r\\e\\\"\\\\\\|", "\007\b\t\n\013\014\r\033\"\\|");
		checkSymbolEscape("\\q", "q");
	}

	@Test void testSymbolNumericEscapes()
	{
		checkSymbolEscape("\\101\\7", "A\007");
		checkSymbolEscape("\\x41;\\xe9", "A;é");
		checkSymbolEscape("\\x{41}\\x{E9}\\o{101}", "AéA");
		checkSymbolEscape("\\x{1F600}", new String(Character.toChars(0x1F600)));
		checkSymbolEscape("\\cA\\c?", "\001\177");
	}

	@Test void testSymbolEscapesStayInsideBars()
	{
		/* an escaped bar does not close the symbol */
		loader = new DataLoader(new StringReader("(|a\\|b| c)"));
		token = loader.next();
		assertEquals(Cons.ofList(Symbol.of("a|b"), Symbol.of("c")), token);
		assertNull(loader.lastIOException());

		/* escapes work in every |...| piece of a symbol */
		loader = new DataLoader(new StringReader("a|\\x41|b|\\n|c"));
		token = loader.next();
		assertEquals(Symbol.of("aAb\nc"), token);
		assertNull(loader.lastIOException());
	}

	@Test void testSymbolBadEscapes()
	{
		loader = new DataLoader(new StringReader("|a\\"));
		assertThrows(DataLoadingException.class, () -> loader.next());

		loader = new DataLoader(new StringReader("|a\\x{41"));
		assertThrows(DataLoadingException.class, () -> loader.next());

		loader = new DataLoader(new StringReader("|\\o41|"));
		assertThrows(DataLoadingException.class, () -> loader.next());
	}

	/* A lone # is rejected, but |#| is a symbol named "#". */
	@Test void testLoneHashIsRejected()
	{
		loader = new DataLoader(new StringReader("#"));
		assertThrows(DataLoadingException.class, () -> loader.next());

		loader = new DataLoader(new StringReader("(a #)"));
		assertThrows(DataLoadingException.class, () -> loader.next());

		loader = new DataLoader(new StringReader("|#|"));
		token = loader.next();
		assertEquals(Symbol.of("#"), token);
	}

	/* Number-like literals are off-limits (consider them "reserved"...) */
	@Test void testNumberLookingLiterals()
	{
		/* Bad */
		loader = new DataLoader(new StringReader("(length 5cm)"));
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.lastIOException());

		/* Better */
		loader = new DataLoader(new StringReader("(length . (cm 5))"));
		token = loader.next();
		assertEquals(Cons.of(Symbol.of("length"),
			Cons.ofList(Symbol.of("cm"), NumberScalar.of("5"))),
			token);
		assertNull(loader.lastIOException());

		/* Not encouraged; but if you must... */
		loader = new DataLoader(new StringReader("(length |5cm|)"));
		token = loader.next();
		assertEquals(Cons.ofList(Symbol.of("length"), Symbol.of("5cm")), token);
		assertNull(loader.lastIOException());
	}

	@Test void testRemoveLimits()
	throws IOException
	{
		DataLoader loader = new DataLoader(new StringReader("((a)) (a b)"))
			.useDepthLimit(0).useLengthLimit(1).noDepthLimit();
		assertEquals(Long.MAX_VALUE, loader.getDepthLimit());
		assertEquals(1, loader.getLengthLimit());
		assertEquals(new DataLoader(new StringReader("((a))")).readDatum(), loader.readDatum());
		loader.noLengthLimit();
		assertEquals(Long.MAX_VALUE, loader.getLengthLimit());
		assertEquals(new DataLoader(new StringReader("(a b)")).readDatum(), loader.readDatum());
	}
}
