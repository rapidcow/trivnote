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
		Scalar world = Scalar.ofString("world");
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
		assertDumps("17/29", Scalar.ofNumber("17/29"));
		assertDumps("-1.5e3", Scalar.ofNumber("-1.5e3"));
		assertDumps("+inf.0", Scalar.ofNumber("+inf.0"));
		assertDumps("#t", TruthScalar.TRUE);
		assertDumps("#f", TruthScalar.FALSE);
	}

	@DisplayName("Strings")
	@Test void testStrings()
	throws
		IOException
	{
		assertDumps("\"\"", Scalar.ofString(""));
		assertDumps("\"hello world\"", Scalar.ofString("hello world"));
		assertDumps("\"a\\\"b\"", Scalar.ofString("a\"b"));
		assertDumps("\"a\\\\b\"", Scalar.ofString("a\\b"));
		assertDumps("\"a\\\\\"", Scalar.ofString("a\\"));
		assertDumps("\"|;()\"", Scalar.ofString("|;()"));
	}

	@DisplayName("Strings with control characters")
	@Test void testStringControls()
	throws
		IOException
	{
		assertDumps("\"\\a\\b\\t\\n\\v\\f\\r\\e\"",
			Scalar.ofString("\007\010\011\012\013\014\015\033"));
		assertDumps("\"\\0\"", Scalar.ofString("\0"));
		assertDumps("\"\\37\"", Scalar.ofString("\037"));
		assertDumps("\"\\177\"", Scalar.ofString("\177"));
		assertDumps("\"\\0001\"", Scalar.ofString("\0001"));
	}

	@DisplayName("Strings with non-ASCII characters")
	@Test void testStringNonASCII()
	throws
		IOException
	{
		assertDumps("\"caf\\351\"", Scalar.ofString("caf\u00e9"));
		assertDumps("\"\\x{4e2d}\\x{6587}\"", Scalar.ofString("\u4e2d\u6587"));
		/* a supplementary character is one escape, not two */
		assertDumps("\"\\x{1f600}\"", Scalar.ofString("\uD83D\uDE00"));
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
			Scalar.ofString("café 記憶 😀\n\u0085\"\\"),
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
}
