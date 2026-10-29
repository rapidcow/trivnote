/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Octet vectors")
class OctetVectorTest
{
	private static Datum
	read(String source)
	throws
		IOException
	{
		return new DataLoader(new StringReader(source)).readDatum();
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

	@Test void testConstructors()
	{
		byte[] raw = {1, 2, (byte) 0xff};
		OctetVector fromArray = OctetVector.of(raw);
		raw[0] = 9;  /* the vector keeps its own copy */
		assertEquals(1, fromArray.get(0));
		assertEquals(255, fromArray.get(2));
		assertEquals(3, fromArray.length());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(1);
		out.write(2);
		out.write(255);
		OctetVector fromStream = OctetVector.of(out);
		out.write(4);  /* ... and so does this one */
		assertEquals(3, fromStream.length());
		assertEquals(fromArray, fromStream);
		assertEquals(fromArray.hashCode(), fromStream.hashCode());
		assertArrayEquals(new byte[] {1, 2, (byte) 0xff}, fromStream.toByteArray());
		assertEquals(0, OctetVector.of().length());
	}

	@Test void testEquality()
	{
		assertEquals(OctetVector.of((byte) 1), OctetVector.of((byte) 1));
		assertNotEquals(OctetVector.of((byte) 1), OctetVector.of((byte) 2));
		assertNotEquals(OctetVector.of((byte) 1), OctetVector.of((byte) 1, (byte) 0));
		assertNotEquals(OctetVector.of(), Nil.INSTANCE);
		assertNotEquals(OctetVector.of((byte) 49), StringScalar.of("1"));
	}

	@Test void testGetOutOfBounds()
	{
		OctetVector v = OctetVector.of((byte) 1);
		assertThrows(IndexOutOfBoundsException.class, () -> v.get(1));
		assertThrows(IndexOutOfBoundsException.class, () -> v.get(-1));
	}

	@Test void testAsOctets()
	{
		Datum v = OctetVector.of((byte) 1);
		assertSame(v, Datum.asOctets(v));
		assertThrows(IllegalArgumentException.class, () -> Datum.asOctets(Nil.INSTANCE));
	}

	@Test void testRead()
	throws
		IOException
	{
		OctetVector want = OctetVector.of((byte) 0, (byte) 127, (byte) 255);
		for (String source : List.of(
			"#u8(0 127 255)", "#u8[0 127 255]", "#u8{0 127 255}",
			"#U8( 0 #x7f #e255.0 )", "#u8(0 127 #;999 255)")
		) {
			assertEquals(want, read(source), source);
		}
		assertEquals(OctetVector.of(), read("#u8()"));
		assertEquals(Cons.of(OctetVector.of((byte) 1), Nil.INSTANCE), read("(#u8(1))"));
	}

	@Test void testReadSplice()
	throws
		IOException
	{
		assertEquals(OctetVector.of((byte) 32, (byte) 97, (byte) 98),
			read("#u8( #x20 #\"ab\" )"));
		assertEquals(OctetVector.of((byte) 1, (byte) 2),
			read("#u8( #u8( #u8( 1 ) 2 ) #\"\" )"));
		assertEquals(OctetVector.of(), read("#u8(#u8())"));
	}

	@Test void testReadSpliceDepth()
	{
		DataLoader reader = new DataLoader(
			new StringReader("#u8(#u8(#u8(1)))"));
		reader.useDepthLimit(2);
		assertThrows(DataLoadingException.class, reader::readDatum);
	}

	@Test void testReadInvalid()
	{
		for (String source : List.of(
			"#u", "#u7()", "#u8", "#u8 (1)", "#u8(1", "#u8(1]",
			"#u8(256)", "#u8(-1)", "#u8(1.5)", "#u8(1/0)",
			"#u8(a)", "#u8(\"1\")", "#u8((1))", "#u8(1 . 2)")
		) {
			DataLoader reader = new DataLoader(new StringReader(source));
			assertThrows(DataLoadingException.class, reader::readDatum, source);
		}
	}

	@Test void testDump()
	throws
		IOException
	{
		assertEquals("#u8()", dump(OctetVector.of()));
		assertEquals("#u8(0 127 255)", dump(OctetVector.of((byte) 0, (byte) 127, (byte) -1)));
		Datum nested = Cons.of(OctetVector.of((byte) 1), Nil.INSTANCE);
		assertEquals("(#u8(1))", dump(nested));
		assertEquals(nested, read(dump(nested)));
	}
}
