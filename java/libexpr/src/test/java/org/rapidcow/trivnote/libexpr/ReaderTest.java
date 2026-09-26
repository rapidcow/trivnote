package org.rapidcow.trivnote.libexpr;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		assertNull(rdr.eio());
	}

	@Test void testReadScalar()
	{
		bff = new StringReader("(2.719 -0.898 .67 -.1 \"1024\" \"abc\")");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				new Scalar("2.719"),
				new Scalar("-0.898"),
				new Scalar(".67"),
				new Scalar("-.1"),
				new Scalar("1024"),
				new Scalar("abc")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
		assertNull(rdr.eio());
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
		assertNull(rdr.eio());
	}

	@Test void testDatumComment()
	{
		bff = new StringReader("(#;A #;(OA deg 30) 42)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				new Scalar("42")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
		assertNull(rdr.eio());
	}

	@Test void testDottedPair()
	{
		bff = new StringReader("(a b . c)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b")
			), list);
		assertEquals(Symbol.of("c"), iter.tail());
		assertNull(rdr.eio());
	}

	@Test void testDottedPairNested()
	{
		/* (a . (b c)) is equivalent to (a b c) */
		bff = new StringReader("(a . (b c))");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of("c")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
		assertNull(rdr.eio());
	}

	@Test void testDottedPairNoSpaceBeforeBracket()
	{
		/*
		 * Dot need not be space-separated from a bracket: `.(c)'
		 * is `. (c)', and consing b onto (c) yields (b c), so the
		 * whole thing flattens to the proper list (a b c).
		 */
		bff = new StringReader("(a b .(c))");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of("c")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
		assertNull(rdr.eio());
	}

	@Test void testDotGluedToAtomIsOrdinarySymbol()
	{
		/*
		 * A `.' immediately followed by a non-delimiter character
		 * is not the pair marker -- it is just the first character
		 * of a symbol.
		 */
		bff = new StringReader("(a b .c)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of(".c")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
		assertNull(rdr.eio());
	}

	@Test void testDotAsOrdinarySymbolChar()
	{
		/* a dot not delimiter-bounded is just a symbol character */
		bff = new StringReader("(... a.b)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("..."),
				Symbol.of("a.b")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
		assertNull(rdr.eio());
	}

	/*
	 * In R7RS there is a grammar rule specifically named
	 * "peculiar identifiers" -- this tests those.
	 *
	 * (It is a mere coincidence some of these symbols
	 * resemble faces.)
	 */
	@Test void testWeirdIdentifiers()
	{
		bff = new StringReader("(+ - +. -. -.o +.=)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("+"),
				Symbol.of("-"),
				Symbol.of("+."),
				Symbol.of("-."),
				Symbol.of("-.o"),
				Symbol.of("+.=")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
		assertNull(rdr.eio());
	}

	@Test void testDottedPairLeadingDotFails()
	{
		bff = new StringReader("(. a)");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
		assertNull(rdr.eio());
	}

	@Test void testDottedPairDoubleDotFails()
	{
		bff = new StringReader("(a . . b)");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
		assertNull(rdr.eio());
	}

	@Test void testDottedPairExcessDatumFails()
	{
		bff = new StringReader("(a . b c)");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
		assertNull(rdr.eio());
	}

	@Test void testDottedPairMissingDatumFails()
	{
		bff = new StringReader("(a . )");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
		assertNull(rdr.eio());
	}

	@Test void testTopLevelDotIsIllegal()
	{
		bff = new StringReader(". a");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
		assertNull(rdr.eio());
	}

	@Test void testSymbolQuote()
	{
		bff = new StringReader("(|foo bar| baz al|ter|na|tion|)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		iter = new Walk(tok);

		list = new ArrayList<>();
		iter.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("foo bar"),
				Symbol.of("baz"),
				Symbol.of("alternation")
			), list);
		assertEquals(Nil.INSTANCE, iter.tail());
		assertNull(rdr.eio());
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
		assertNull(rdr.eio());
	}
}
