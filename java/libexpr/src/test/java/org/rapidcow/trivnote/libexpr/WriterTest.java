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
}
