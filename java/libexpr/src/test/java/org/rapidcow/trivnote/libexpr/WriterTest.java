/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package org.rapidcow.trivnote.libexpr;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Data Dumper test")
class WriterTest
{
	@DisplayName("Hello world")
	@Test void testHelloWorld()
	throws
		IOException
	{
		Symbol hello = Symbol.of("hello");
		Datum world = StringScalar.of("world");
		Cons rootNode = Cons.of(hello, world);
		StringWriter ws = new StringWriter();
		DataDumper dd = new DataDumper(ws);
		dd.writeDatum(rootNode);
		String fmt = ws.toString();
		assertEquals("(hello . \"world\")", fmt);

		StringReader rs = new StringReader(fmt);
		DataLoader ld = new DataLoader(rs);
		Datum node = ld.readDatum();
		assertEquals(rootNode, node);
	}

	private static String
	dump(Datum node)
	throws
		IOException
	{
		StringWriter ws = new StringWriter();
		new DataDumper(ws).writeDatum(node);
		return ws.toString();
	}

	/**
	 * Dumps {@code node}, checks the output and that it reads back
	 * as an equal datum.
	 */
	private static void
	assertDumps(String expect, Datum node)
	throws
		IOException
	{
		String fmt = dump(node);
		assertEquals(expect, fmt);
		assertEquals(node, new DataLoader(new StringReader(fmt)).readDatum());
	}

	@DisplayName("Bare symbols")
	@Test void testPlainSymbols()
	throws
		IOException
	{
		for (String name : new String[] {
			"hello", "+", "-", "...", "..", "a.b", "a#b", "-.cm",
			"+inf", "nan.0", "+NaN.0", "a'b", "a\\b"
		}) {
			assertDumps(name, Symbol.of(name));
		}
	}

	@DisplayName("Symbols that need |...|")
	@Test void testQuotedSymbols()
	throws
		IOException
	{
		assertDumps("||", Symbol.of(""));
		assertDumps("|.|", Symbol.of("."));
		assertDumps("|hello world|", Symbol.of("hello world"));
		assertDumps("|a(b|", Symbol.of("a(b"));
		assertDumps("|a;b|", Symbol.of("a;b"));
		assertDumps("|a\"b|", Symbol.of("a\"b"));
		assertDumps("|#t|", Symbol.of("#t"));
		assertDumps("|#|", Symbol.of("#"));
		assertDumps("|a\\|b|", Symbol.of("a|b"));
		assertDumps("|a b\\\\c|", Symbol.of("a b\\c"));
	}

	@DisplayName("Symbols with control characters")
	@Test void testSymbolControls()
	throws
		IOException
	{
		assertDumps("|a\\nb|", Symbol.of("a\nb"));
		assertDumps("|\\a\\b\\t\\n\\v\\f\\r\\e|",
			Symbol.of("\007\010\011\012\013\014\015\033"));
		assertDumps("|\\0|", Symbol.of("\0"));
		assertDumps("|\\177|", Symbol.of("\177"));
		assertDumps("|\\37z|", Symbol.of("\037z"));
	}

	@DisplayName("Symbols with non-ASCII characters")
	@Test void testSymbolNonASCII()
	throws
		IOException
	{
		assertDumps("|caf\\351|", Symbol.of("café"));
		assertDumps("|\\x{8a18}\\x{61b6}|", Symbol.of("記憶"));
		assertDumps("|\\x{1f600}|", Symbol.of("😀"));
	}

	@DisplayName("Symbols that look like numbers")
	@Test void testNumericSymbols()
	throws
		IOException
	{
		for (String name : new String[] {
			"1", "1cm", "1+", "+1", "-1", "+4x", ".5", ".5x", "-.4cm",
			"+nan.0", "-nan.0", "+inf.0", "-inf.0"
		}) {
			assertDumps("|" + name + "|", Symbol.of(name));
		}
	}

	@DisplayName("Non-string scalars are written as is")
	@Test void testOtherScalars()
	throws
		IOException
	{
		assertDumps("17/29", NumberScalar.of("17/29"));
		assertDumps("-1.5e3", NumberScalar.of("-1.5e3"));
		assertDumps("+inf.0", NumberScalar.of("+inf.0"));
		assertDumps("#t", BoolScalar.TRUE);
		assertDumps("#f", BoolScalar.FALSE);
	}

	@DisplayName("Strings")
	@Test void testStrings()
	throws
		IOException
	{
		assertDumps("\"\"", StringScalar.of(""));
		assertDumps("\"hello world\"", StringScalar.of("hello world"));
		assertDumps("\"a\\\"b\"", StringScalar.of("a\"b"));
		assertDumps("\"a\\\\b\"", StringScalar.of("a\\b"));
		assertDumps("\"a\\\\\"", StringScalar.of("a\\"));
		assertDumps("\"|;()\"", StringScalar.of("|;()"));
	}

	@DisplayName("Strings with control characters")
	@Test void testStringControls()
	throws
		IOException
	{
		assertDumps("\"\\a\\b\\t\\n\\v\\f\\r\\e\"",
			StringScalar.of("\007\010\011\012\013\014\015\033"));
		assertDumps("\"\\0\"", StringScalar.of("\0"));
		assertDumps("\"\\37\"", StringScalar.of("\037"));
		assertDumps("\"\\177\"", StringScalar.of("\177"));
		assertDumps("\"\\0001\"", StringScalar.of("\0001"));
	}

	@DisplayName("Strings with non-ASCII characters")
	@Test void testStringNonASCII()
	throws
		IOException
	{
		assertDumps("\"caf\\351\"", StringScalar.of("caf\u00e9"));
		assertDumps("\"\\x{4e2d}\\x{6587}\"", StringScalar.of("\u4e2d\u6587"));
		/* a supplementary character is one escape, not two */
		assertDumps("\"\\x{1f600}\"", StringScalar.of("\uD83D\uDE00"));
	}

	@DisplayName("Test load/dump limit congruency")
	@Test void testLimitsCongruency()
	throws IOException
	{
		for (String source : new String[] {"a", "()", "(a)", "(a b . c)", "(a (b))", "(())"}) {
			Datum node = new DataLoader(new StringReader(source)).readDatum();
			for (long depth = 0; depth <= 3; depth++) {
				for (long length = 0; length <= 3; length++) {
					StringWriter out = new StringWriter();
					DataDumper dumper = new DataDumper(out)
						.useDepthLimit(depth).useLengthLimit(length);
					DataLoader loader = new DataLoader(new StringReader(source))
						.useDepthLimit(depth).useLengthLimit(length);
					try {
						loader.readDatum();
					} catch (DataLoadingException e) {
						assertThrows(DataDumpingException.class, () -> dumper.writeDatum(node));
						continue;
					}
					dumper.writeDatum(node);
					assertEquals(node, new DataLoader(new StringReader(out.toString()))
						.useDepthLimit(depth).useLengthLimit(length).readDatum());
				}
			}
		}
	}

	@Test void testLimitConfigurationAndReuse()
	throws IOException
	{
		StringWriter out = new StringWriter();
		DataDumper dumper = new DataDumper(out);
		assertThrows(IllegalArgumentException.class, () -> dumper.useDepthLimit(-1));
		assertThrows(IllegalArgumentException.class, () -> dumper.useLengthLimit(-1));
		dumper.useDepthLimit(1).useLengthLimit(1);
		Datum nested = new DataLoader(new StringReader("((a))")).readDatum();
		assertThrows(DataDumpingException.class, () -> dumper.writeDatum(nested));
		out.getBuffer().setLength(0);
		dumper.writeDatum(Nil.INSTANCE);
		dumper.writeDatum(Nil.INSTANCE);
		assertEquals("()()", out.toString());
		dumper.noMaxDepth();
		out.getBuffer().setLength(0);
		dumper.writeDatum(nested);
		assertEquals("((a))", out.toString());
		Datum longer = new DataLoader(new StringReader("(a b)")).readDatum();
		assertThrows(DataDumpingException.class, () -> dumper.writeDatum(longer));
		dumper.noMaxLength();
		out.getBuffer().setLength(0);
		dumper.writeDatum(longer);
		assertEquals("(a b)", out.toString());
	}

	@Test void testUnicode()
	throws IOException
	{
		for (Datum node : new Datum[] {
			StringScalar.of("café 記憶 😀\n\u0085\"\\"),
			Symbol.of("café 記憶 😀\n\u0085|\\")
		}) {
			StringWriter out = new StringWriter();
			DataDumper dumper = new DataDumper(out).useUnicode(true);
			dumper.writeDatum(node);
			String quote = node instanceof Symbol ? "|" : "\"";
			assertEquals(quote + "café 記憶 😀\\n\\205\\" + quote + "\\\\" + quote, out.toString());
			assertEquals(node, new DataLoader(new StringReader(out.toString())).readDatum());
			out.getBuffer().setLength(0);
			dumper.useUnicode(false).writeDatum(node);
			assertEquals(dump(node), out.toString());
		}
	}

	/** A cons cell that picks its own delimiter. */
	private static final
	class
		Cell
	extends
		Cons
	{
		private final Nil delim;
		private final Datum car, cdr;

		Cell(Nil delim, Datum car, Datum cdr)
		{
			this.delim = delim;
			this.car = car;
			this.cdr = cdr;
		}

		@Override
		public Nil
		delimiter()
		{
			return delim;
		}

		@Override
		public Datum
		car()
		{
			return car;
		}

		@Override
		public Datum
		cdr()
		{
			return cdr;
		}
	}

	/** Makes {@code delim}-delimited list of the elements. */
	private static Datum
	listOf(Nil delim, Datum... items)
	{
		Datum rest = delim;
		for (int i = items.length - 1; i >= 0; i--) {
			rest = new Cell(delim, items[i], rest);
		}
		return rest;
	}

	@DisplayName("Delimiters follow the cons chain")
	@Test void testDelimiters()
	throws
		IOException
	{
		Symbol a = Symbol.of("a"), b = Symbol.of("b"), c = Symbol.of("c");
		assertEquals("[a b c]", dump(listOf(Nil.BRACK, a, b, c)));
		assertEquals("{a b}", dump(listOf(Nil.CURLY, a, b)));
		assertEquals("#(a b)", dump(ConsVector.of(a, b)));
		assertEquals("#{a}", dump(listOf(Nil.ALT_CURLY, a)));
		/* every flavor of nil on its own */
		assertEquals("()", dump(Nil.PAREN));
		assertEquals("[]", dump(Nil.BRACK));
		assertEquals("{}", dump(Nil.CURLY));
		assertEquals("#()", dump(Nil.ALT_PAREN));
		assertEquals("#[]", dump(Nil.ALT_BRACK));
		assertEquals("#{}", dump(Nil.ALT_CURLY));
	}

	@DisplayName("Terminators are inferred only from the same delimiter")
	@Test void testTypedTerminators()
	throws
		IOException
	{
		Symbol a = Symbol.of("a"), b = Symbol.of("b");
		assertDumps("(a b)", Cons.of(a, b, Nil.PAREN));
		assertEquals("(a . [])", dump(new Cell(Nil.PAREN, a, Nil.BRACK)));
		assertEquals("[a . ()]", dump(new Cell(Nil.BRACK, a, Nil.PAREN)));
		assertEquals("[a b . ()]", dump(
			new Cell(Nil.BRACK, a, new Cell(Nil.BRACK, b, Nil.PAREN))));
		assertEquals("(a . #())", dump(new Cell(Nil.PAREN, a, Nil.ALT_PAREN)));
		assertEquals("#(a . ())", dump(new Cell(Nil.ALT_PAREN, a, Nil.PAREN)));
		assertEquals("[a . b]", dump(new Cell(Nil.BRACK, a, b)));
	}

	@DisplayName("A cdr with another delimiter is a dotted tail")
	@Test void testSwitchingDelimiter()
	throws
		IOException
	{
		Symbol a = Symbol.of("a"), b = Symbol.of("b"), c = Symbol.of("c");
		assertEquals("(a . [b c])",
			dump(new Cell(Nil.PAREN, a, listOf(Nil.BRACK, b, c))));
		assertEquals("[a b . (c)]",
			dump(new Cell(Nil.BRACK, a,
				new Cell(Nil.BRACK, b, listOf(Nil.PAREN, c)))));
		assertEquals("(a . #(b c))",
			dump(new Cell(Nil.PAREN, a, ConsVector.of(b, c))));
	}

	@DisplayName("Depth counts every level, whatever the delimiter")
	@Test void testAlternatingDepth()
	throws
		IOException
	{
		Symbol a = Symbol.of("a");
		Datum node = listOf(Nil.PAREN, a,
			listOf(Nil.BRACK, a,
				listOf(Nil.CURLY, a,
					ConsVector.of(a, Nil.ALT_CURLY))));
		assertEquals("(a [a {a #(a #{})}])", dump(node));
		StringWriter out = new StringWriter();
		new DataDumper(out).useDepthLimit(5).writeDatum(node);
		assertEquals("(a [a {a #(a #{})}])", out.toString());
		DataDumper strict = new DataDumper(new StringWriter()).useDepthLimit(4);
		assertThrows(DataDumpingException.class, () -> strict.writeDatum(node));
		/* an empty list of any flavor still needs depth one */
		DataDumper none = new DataDumper(new StringWriter()).useDepthLimit(0);
		for (Nil nil : Nil.values()) {
			assertThrows(DataDumpingException.class, () -> none.writeDatum(nil));
		}
	}

	@DisplayName("Length limit applies to non-paren lists")
	@Test void testLengthLimitDelimiters()
	throws
		IOException
	{
		Symbol a = Symbol.of("a");
		DataDumper dumper = new DataDumper(new StringWriter()).useLengthLimit(2);
		assertThrows(DataDumpingException.class,
			() -> dumper.writeDatum(ConsVector.of(a, a, a)));
		assertThrows(DataDumpingException.class,
			() -> dumper.writeDatum(listOf(Nil.BRACK, a, a, a)));
	}
}
