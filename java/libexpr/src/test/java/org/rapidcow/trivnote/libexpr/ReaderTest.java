package org.rapidcow.trivnote.libexpr;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
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
	}

	@Test void testDottedPairLeadingDotFails()
	{
		bff = new StringReader("(. a)");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
	}

	@Test void testDottedPairDoubleDotFails()
	{
		bff = new StringReader("(a . . b)");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
	}

	@Test void testDottedPairExcessDatumFails()
	{
		bff = new StringReader("(a . b c)");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
	}

	@Test void testDottedPairMissingDatumFails()
	{
		bff = new StringReader("(a . )");
		rdr = new DataLoader(bff);
		assertThrows(DataLoader.ParseException.class, () -> rdr.next());
	}

	@Test void testTopLevelDotIsOrdinarySymbol()
	{
		/*
		 * The dot is only a pair separator inside a bracketed list;
		 * at the top level it is read as an ordinary symbol.
		 */
		bff = new StringReader(". a");
		rdr = new DataLoader(bff);
		assertEquals(Symbol.of("."), rdr.next());
		assertEquals(Symbol.of("a"), rdr.next());
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
