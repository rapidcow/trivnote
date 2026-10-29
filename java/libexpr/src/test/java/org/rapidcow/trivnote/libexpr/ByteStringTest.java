/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Byte strings #\"...\" and #u8\"...\"")
class ByteStringTest
{
	private static OctetVector
	octets(int... values)
	{
		byte[] bytes = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			bytes[i] = (byte) values[i];
		}
		return OctetVector.of(bytes);
	}

	private static void
	check(String source, int... expected)
	throws
		IOException
	{
		for (String prefix : List.of("#\"", "#u8\"")) {
			DataLoader reader = new DataLoader(
				new StringReader(prefix + source + "\""));
			assertEquals(octets(expected), reader.readDatum(), source);
		}
	}

	private static void
	checkInvalid(String source)
	{
		for (String prefix : List.of("#\"", "#u8\"")) {
			DataLoader reader = new DataLoader(
				new StringReader(prefix + source + "\""));
			assertThrows(DataLoadingException.class,
				reader::readDatum, source);
		}
	}

	@Test void testPlainAndSimpleEscapes()
	throws
		IOException
	{
		check("");
		check("AB\\n\\t\\s\\\\\\\"", 65, 66, 10, 9, 32, 92, 34);
		check("\\cA\\^[\\^?", 1, 27, 127);
		check("a\\\n   b", 97, 98);
	}

	@Test void testUnbracedNumericEscapes()
	throws
		IOException
	{
		check("\\x41\\x4g\\xe9", 65, 4, 'g', 0xe9);
		check("\\101\\0\\377", 65, 0, 255);
		check("\\o41", 33);
		check("\\o1011", 65, '1');
		check("\\d065\\d9z", 65, 9, 'z');
		check("\\m01000001\\m1", 65, 1);
	}

	@Test void testBracedBundles()
	throws
		IOException
	{
		check("\\x{}", 0);
		check("\\x{f}\\x{ff}", 15, 255);
		check("\\x{0dc82b1d-2fac-4a70-a4c0-60ef920dba83}",
			0x0d, 0xc8, 0x2b, 0x1d, 0x2f, 0xac, 0x4a, 0x70,
			0xa4, 0xc0, 0x60, 0xef, 0x92, 0x0d, 0xba, 0x83);
		check("\\x{ 4 1 _ 4 2 }", 0x41, 0x42);
		check("\\d{192.168.0.1}", 192, 168, 0, 1);
		check("\\d{065066}", 65, 66);
		check("\\x{2001:0db8}", 0x20, 0x01, 0x0d, 0xb8);
		check("\\o{101 102}", 65, 66);
		check("\\m{01000001 01000010}", 65, 66);
		check("\\m{1000001}", 65);
	}

	@Test void testBracedComments()
	throws
		IOException
	{
		check("\\x{\n 89  ; random guy\n 50 4E 47 ; PNG\n 0D 0A\n}",
			0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a);
	}

	@Test void testNameEscape()
	throws
		IOException
	{
		check("\\N{LATIN SMALL LETTER A}\\N{ return }\\N{null}", 97, 13, 0);
		check("\\N{LATIN SMALL LETTER E WITH ACUTE}", 0xe9);
		check("\\N{LATIN SMALL LETTER Y WITH DIAERESIS}", 0xff);
	}

	@Test void testBase64()
	throws
		IOException
	{
		check("\\B{}");
		check("\\B{QUJD}", 65, 66, 67);
		check("\\B{QUI=}\\B{QUI}", 65, 66, 65, 66);
		check("\\B{QQ==}\\B{QQ}\\B{QQ..}", 65, 65, 65);
		check("\\B{ Q U\nJ\tD ; comment\n }", 65, 66, 67);
		check("\\B{-_-_}\\B{+/+/}", 0xfb, 0xff, 0xbf, 0xfb, 0xff, 0xbf);
	}

	@Test void testInvalid()
	{
		for (String source : List.of(
			"\\N{GRINNING FACE}", "\\N{no such name}", "\\N", "\\N{", "\\B", "\\B{QUJD", "\\B{Q}",
			"\\B{Q*JD}", "\\B{QQ==QQ}",
			"\\x{100}", "\\d{256}", "\\d256", "\\o{400}", "\\m{100000000}",
			"\\x{123}", "\\d{1234}", "\\x{41:}", "\\x{:41}", "\\x{41::42}",
			"\\x{g}", "\\x{41", "\\x{41 ; no newline}",
			"\u00e9", "\\\u00e9", "\\x{1F600}")
		) {
			checkInvalid(source);
		}
	}

	@Test void testUnterminated()
	{
		for (String source : List.of("#\"abc", "#u8\"abc", "#\"abc\\", "#\""))
		{
			DataLoader reader = new DataLoader(new StringReader(source));
			assertThrows(DataLoadingException.class,
				reader::readDatum, source);
		}
	}
}
