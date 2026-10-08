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
	private Reader input;
	private Datum token;
	private DataLoader loader;
	private List<Datum> list;
	private Walk walk;

	@Test void testReadSymbol()
	{
		input = new StringReader("(foo bar baz)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("foo"),
				Symbol.of("bar"),
				Symbol.of("baz")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testReadScalar()
	{
		input = new StringReader("(2.719 -0.898 .67 -.1 \"1024\" \"abc\")");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				new Scalar("2.719"),
				new Scalar("-0.898"),
				new Scalar(".67"),
				new Scalar("-.1"),
				new Scalar("1024"),
				new Scalar("abc")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testLineComment()
	{
		input = new StringReader("(abc;comment\ndef)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("abc"),
				Symbol.of("def")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDatumComment()
	{
		input = new StringReader("(#;A #;(OA deg 30) 42)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				new Scalar("42")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDottedPair()
	{
		input = new StringReader("(a b . c)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b")
			), list);
		assertEquals(Symbol.of("c"), walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDottedPairNested()
	{
		/* (a . (b c)) is equivalent to (a b c) */
		input = new StringReader("(a . (b c))");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of("c")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDottedPairNoSpaceBeforeBracket()
	{
		/*
		 * Dot need not be space-separated from a bracket: `.(c)'
		 * is `. (c)', and consing b onto (c) yields (b c), so the
		 * whole thing flattens to the proper list (a b c).
		 */
		input = new StringReader("(a b .(c))");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of("c")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDotGluedToAtomIsOrdinarySymbol()
	{
		/*
		 * A `.' immediately followed by a non-delimwalk character
		 * is not the pair marker -- it is just the first character
		 * of a symbol.
		 */
		input = new StringReader("(a b .c)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("a"),
				Symbol.of("b"),
				Symbol.of(".c")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDotAsOrdinarySymbolChar()
	{
		/* a dot not delimwalk-bounded is just a symbol character */
		input = new StringReader("(... a.b)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("..."),
				Symbol.of("a.b")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
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
		input = new StringReader("(+ - +. -. -.o +.=)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("+"),
				Symbol.of("-"),
				Symbol.of("+."),
				Symbol.of("-."),
				Symbol.of("-.o"),
				Symbol.of("+.=")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDottedPairLeadingDotFails()
	{
		input = new StringReader("(. a)");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.eio());
	}

	@Test void testDottedPairDoubleDotFails()
	{
		input = new StringReader("(a . . b)");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.eio());
	}

	@Test void testDottedPairExcessDatumFails()
	{
		input = new StringReader("(a . b c)");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.eio());
	}

	@Test void testDottedPairMissingDatumFails()
	{
		input = new StringReader("(a . )");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.eio());
	}

	@Test void testTopLevelDotIsIllegal()
	{
		input = new StringReader(". a");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.eio());
	}

	@Test void testTopLevelDotAtEofIsIllegal()
	{
		/* used to read as the symbol `.' */
		input = new StringReader(".");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.eio());
	}

	@Test void testDottedPairDotAtEofFails()
	{
		input = new StringReader("(a .");
		loader = new DataLoader(input);
		assertThrows(DataLoadingException.class, () -> loader.next());
		assertNull(loader.eio());
	}

	@Test void testDottedPairNoSpaceBeforeQuote()
	{
		/* `.' followed by `"' is the pair marker, as with a bracket */
		input = new StringReader("(a .\"b\")");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(List.of(Symbol.of("a")), list);
		assertEquals(new Scalar("b"), walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDottedPairNoSpaceBeforeBar()
	{
		input = new StringReader("(a .|b c|)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(List.of(Symbol.of("a")), list);
		assertEquals(Symbol.of("b c"), walk.tail());
		assertNull(loader.eio());
	}

	@Test void testDottedPairDotBeforeLineComment()
	{
		input = new StringReader("(a .;comment\nb)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(List.of(Symbol.of("a")), list);
		assertEquals(Symbol.of("b"), walk.tail());
		assertNull(loader.eio());
	}

	@Test void testSymbolQuote()
	{
		input = new StringReader("(|foo bar| baz al|ter|na|tion|)");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				Symbol.of("foo bar"),
				Symbol.of("baz"),
				Symbol.of("alternation")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}

	@Test void testStringQuote()
	{
		/* ("say \"hi\"" "back\\slash" "line1\nline2") */
		input = new StringReader(
			"(\"say \\\"hi\\\"\" \"back\\\\slash\" \"line1\\nline2\")");
		loader = new DataLoader(input);
		token = loader.next();
		walk = new Walk(token);

		list = new ArrayList<>();
		walk.forEachRemaining(list::add);
		assertIterableEquals(
			List.of(
				new Scalar("say \"hi\""),
				new Scalar("back\\slash"),
				new Scalar("line1\nline2")
			), list);
		assertEquals(Nil.INSTANCE, walk.tail());
		assertNull(loader.eio());
	}
}
