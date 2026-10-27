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
	private static void check(String source, String expected)
	{
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			DataLoader reader = new DataLoader(
				new StringReader("\"" + source + "\""));
			assertEquals(StringScalar.of(expected), reader.next(), source);
		});
	}

	@Test void testSpacesBetweenDigits()
	{
		check("\\x{ 4 0 }", "@");
		check("\\x{4\t1}", "A");
		check("\\o{1 0 1}", "A");
	}

	@Test void testSeparatorsBetweenDigits()
	{
		check("\\x{4-1}", "A");
		check("\\x{4_1}", "A");
		check("\\x{1f_600}", "\uD83D\uDE00");
		check("\\x{-_ 4 - 1 _-}", "A");
	}
}
