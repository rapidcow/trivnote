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
}
