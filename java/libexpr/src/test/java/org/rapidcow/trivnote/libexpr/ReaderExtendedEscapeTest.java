/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.StringReader;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Extensions to braced escapes beyond what Perl allows: whitespace,
 * {@code -} and {@code _} may appear between digits and are ignored.
 */
@DisplayName("Extended braced escapes")
class ReaderExtendedEscapeTest
{
	private static void checkStringEscape(String source, String expected)
	{
		DataLoader reader = new DataLoader(
			new StringReader("\"" + source + "\""));
		assertEquals(StringScalar.of(expected), reader.next(), source);
	}

	@Test void testSpacesBetweenDigits()
	{
		checkStringEscape("\\x{ 4 0 }", "@");
		checkStringEscape("\\x{4\t1}", "A");
		checkStringEscape("\\o{1 0 1}", "A");
	}

	@Test void testSeparatorsBetweenDigits()
	{
		checkStringEscape("\\x{4-1}", "A");
		checkStringEscape("\\x{4_1}", "A");
		checkStringEscape("\\x{1f_600}", "\uD83D\uDE00");
		checkStringEscape("\\x{-_ 4 - 1 _-}", "A");
	}
}
