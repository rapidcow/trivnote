package org.rapidcow.trivnote.libexpr;

import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReaderTest
{
	private Reader bff;
	private Datum tok;
	private DataLoader rdr;

	@Test void testReadSymbol()
	{
		bff = new StringReader("(foo bar baz)");
		rdr = new DataLoader(bff);
		tok = rdr.next();
		assertEquals(true, tok instanceof Cons);
		assertEquals(Symbol.of("foo"),
			((Cons) tok).car());
		assertEquals(Symbol.of("bar"),
			((Cons)
				((Cons) tok).cdr()
			).car());
		assertEquals(Symbol.of("baz"),
			((Cons)
				((Cons)
					((Cons) tok).cdr()
				).cdr()
			).car());
		assertEquals(Nil.INSTANCE,
			((Cons)
				((Cons)
					((Cons) tok).cdr()
				).cdr()
			).cdr());
	}
}
