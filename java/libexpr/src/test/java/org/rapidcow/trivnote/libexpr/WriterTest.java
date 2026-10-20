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
			"+inf", "nan.0", "+NaN.0", "a'b", "a\\b", "\u00e9"
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
		assertDumps("|a\nb|", Symbol.of("a\nb"));
		assertDumps("|#t|", Symbol.of("#t"));
		assertDumps("|#|", Symbol.of("#"));
		assertDumps("|a\\|b|", Symbol.of("a|b"));
		assertDumps("|a b\\\\c|", Symbol.of("a b\\c"));
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
		assertDumps("\"\\x{0}\"", Scalar.ofString("\0"));
		assertDumps("\"\\x{1F}\"", Scalar.ofString("\037"));
		assertDumps("\"\\x{7F}\"", Scalar.ofString("\177"));
		assertDumps("\"\\x{0}1\"", Scalar.ofString("\0001"));
	}

	@DisplayName("Strings with non-ASCII characters")
	@Test void testStringNonASCII()
	throws
		IOException
	{
		assertDumps("\"caf\\x{E9}\"", Scalar.ofString("caf\u00e9"));
		assertDumps("\"\\x{4E2D}\\x{6587}\"", Scalar.ofString("\u4e2d\u6587"));
		/* a supplementary character is one escape, not two */
		assertDumps("\"\\x{1F600}\"", Scalar.ofString("\uD83D\uDE00"));
	}
}
