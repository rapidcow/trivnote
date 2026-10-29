/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Characters")
class CharScalarTest
{
	private static Datum
	read(String source)
	throws
		IOException
	{
		return new DataLoader(new StringReader(source)).readDatum();
	}

	private static void
	check(String source, int codePoint)
	throws
		IOException
	{
		assertEquals(CharScalar.of(codePoint), read(source), source);
	}

	@Test void testScalar()
	{
		assertEquals(65, CharScalar.of(65).codePointValue());
		assertEquals(CharScalar.of(65), CharScalar.of('A'));
		assertNotEquals(CharScalar.of(65), CharScalar.of(66));
		assertEquals(CharScalar.of(65).hashCode(), CharScalar.of(65).hashCode());
		assertThrows(IllegalArgumentException.class, () -> CharScalar.of(-1));
		assertThrows(IllegalArgumentException.class, () -> CharScalar.of(0x110000));
		assertEquals(CharScalar.of(65), Datum.asChar(CharScalar.of(65)));
		assertThrows(IllegalArgumentException.class, () -> Datum.asChar(Nil.INSTANCE));
	}

	@Test void testReadSingle()
	throws
		IOException
	{
		check("#\\a", 'a');
		check("#\\A ", 'A');
		check("#\\ ", ' ');
		check("#\\(", '(');
		check("#\\;", ';');
		check("#\\x", 'x');
		check("#\\m", 'm');
		check("#\\o", 'o');
		check("#\\d", 'd');
		check("#\\N", 'N');
		check("#\\c", 'c');
		check("#\\^", '^');
		check("#\\é", 0xe9);
		check("#\\😀", 0x1f600);
	}

	@Test void testReadNames()
	throws
		IOException
	{
		check("#\\alarm", 7);
		check("#\\backspace", 8);
		check("#\\delete", 0x7f);
		check("#\\escape", 0x1b);
		check("#\\newline", 10);
		check("#\\null", 0);
		check("#\\return", 13);
		check("#\\space", 32);
		check("#\\tab", 9);
	}

	@Test void testReadNumeric()
	throws
		IOException
	{
		check("#\\x41", 'A');
		check("#\\x1F600", 0x1f600);
		check("#\\x000041", 'A');
		check("#\\o101", 'A');
		check("#\\o373000", 0x1f600);
		check("#\\d65", 'A');
		check("#\\d128512", 0x1f600);
		check("#\\m1000001", 'A');
		check("#\\cJ", 10);
		check("#\\^J", 10);
		check("#\\^?", 127);
		check("#\\c@", 0);
	}

	@Test void testReadBraced()
	throws
		IOException
	{
		check("#\\x{41}", 'A');
		check("#\\x{ 1F_600 }", 0x1f600);
		check("#\\x{}", 0);
		check("#\\o{101}", 'A');
		check("#\\d{ 6 5 }", 'A');
		check("#\\m{100 0001}", 'A');
		check("#\\x{\n 4 ; the four\n 1 ; the one\n}", 'A');
		check("#\\N{LATIN SMALL LETTER A}", 'a');
		check("#\\N{  grinning face \t}", 0x1f600);
		check("#\\N{return}", 13);
		check("#\\N{ Newline }", 10);
		check("#\\N{null}", 0);
	}

	@Test void testReadTermination()
	throws
		IOException
	{
		assertEquals(Cons.of(CharScalar.of('a'), Cons.of(CharScalar.of(' '),
			Nil.INSTANCE)), read("(#\\a #\\space)"));
		assertEquals(Cons.of(CharScalar.of(')'), Nil.INSTANCE), read("(#\\))"));
		assertEquals(Cons.of(CharScalar.of('x'), Nil.INSTANCE), read("(#\\x)"));
		assertEquals(Cons.of(CharScalar.of('A'), Cons.of(CharScalar.of('B'),
			Nil.INSTANCE)), read("(#\\x{41}#\\x{42})"));
	}

	@Test void testReadInvalid()
	{
		for (String source : List.of(
			"#\\", "#\\foo", "#\\Space", "#\\xg", "#\\x41g", "#\\o8", "#\\m2",
			"#\\Nfoo", "#\\N{no such character}", "#\\N{", "#\\x{41", "#\\x{g}",
			"#\\x{41.42}", "#\\x{41:42}", "#\\x110000", "#\\x{110000}",
			"#\\x99999999999", "#\\c\u0080", "#\\cab")
		) {
			DataLoader reader = new DataLoader(new StringReader(source));
			assertThrows(DataLoadingException.class, reader::readDatum, source);
		}
	}

	@Test void testNameEscapeAliases()
	throws
		IOException
	{
		assertEquals(StringScalar.of("a\rb"), read("\"a\\N{return}b\""));
		assertEquals(StringScalar.of("\t\0"), read("\"\\N{TAB}\\N{null}\""));
	}

	@Test void testSpliceIntoOctets()
	throws
		IOException
	{
		assertEquals(OctetVector.of((byte) 'a', (byte) 0xe9, (byte) 0x00,
			(byte) 0xff, (byte) 1),
			read("#u8( #\\a #\\é #\\x{100} #\\xff 1 )"));
		assertEquals(OctetVector.of((byte) 0x00, (byte) 0x41),
			read("#u8( #\\x{1F600} #\\x{D841} )"));
	}

	@Test void testSpliceStringsAsUtf8()
	throws
		IOException
	{
		assertEquals(OctetVector.of((byte) 'a', (byte) 0xf0, (byte) 0x9f,
			(byte) 0x98, (byte) 0x80, (byte) 0xc3, (byte) 0xa9, (byte) 0xe9),
			read("#u8( #\\a \"\\N{GRINNING FACE}\u00e9\" #\\\u00e9 )"));
		assertEquals(OctetVector.of(), read("#u8( \"\" )"));
	}

	private static String
	dump(Datum node, boolean unicode)
	throws
		IOException
	{
		StringWriter ws = new StringWriter();
		DataDumper dumper = new DataDumper(ws);
		dumper.useUnicode(unicode);
		dumper.writeDatum(node);
		return ws.toString();
	}

	@Test void testDump()
	throws
		IOException
	{
		assertEquals("#\\a", dump(CharScalar.of('a'), false));
		assertEquals("#\\space", dump(CharScalar.of(' '), false));
		assertEquals("#\\newline", dump(CharScalar.of('\n'), false));
		assertEquals("#\\null", dump(CharScalar.of(0), false));
		assertEquals("#\\x1", dump(CharScalar.of(1), false));
		assertEquals("#\\xe9", dump(CharScalar.of(0xe9), false));
		assertEquals("#\\é", dump(CharScalar.of(0xe9), true));
		assertEquals("#\\x1f600", dump(CharScalar.of(0x1f600), false));
		for (int cp : new int[] { 'a', ' ', '(', ';', 'x', 0, 1, 0x7f, 0xe9,
			0x1f600, 0xd800, 0x10ffff })
		{
			for (boolean unicode : new boolean[] { false, true }) {
				String text = dump(CharScalar.of(cp), unicode);
				assertEquals(CharScalar.of(cp), read(text), text);
			}
		}
	}
}
