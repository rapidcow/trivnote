package org.rapidcow.trivnote.libexpr;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class ReaderTest
{
	private Reader bff;
	private Datum tok;
	private DataLoader rdr;
	private List<Datum> list;
	private Walk iter;

	@Test void testReadSymbol()
	{
		bff = new StringReader("(foo bar baz)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("foo"),
				Symbol.of("bar"),
				Symbol.of("baz")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
	}

	@Test void testReadScalar()
	{
		bff = new StringReader("(2.719 -0.898 \"1024\")");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				new Scalar("2.719"),
				new Scalar("-0.898"),
				new Scalar("1024")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
	}

	@Test void testLineComment()
	{
		bff = new StringReader("(abc;comment\ndef)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("abc"),
				Symbol.of("def")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
	}

	@Test void testSymbolQuote()
	{
		bff = new StringReader("(|foo bar| baz)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("foo bar"),
				Symbol.of("baz")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
	}

	@Test void testStringQuote()
	{
		/* ("say \"hi\"" "back\\slash" "line1\nline2") */
		bff = new StringReader(
			"(\"say \\\"hi\\\"\" \"back\\\\slash\" \"line1\\nline2\")");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				new Scalar("say \"hi\""),
				new Scalar("back\\slash"),
				new Scalar("line1\nline2")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
	}
}
