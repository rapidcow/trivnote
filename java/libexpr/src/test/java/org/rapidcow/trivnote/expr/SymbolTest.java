package org.rapidcow.trivnote.expr;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SymbolTest
{
	@Test void uselessTest()
	{
		/*
		 * N.B. JUnit puts "expect" before "actual"
		 */
		assertEquals(true, new Symbol.Atom().isAtom());
		assertEquals(false, new Symbol.Cons().isAtom());
	}
}
