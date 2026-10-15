/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalkTest
{
	static final Datum A = Symbol.of("a");
	static final Datum B = Symbol.of("b");
	static final Datum C = Symbol.of("c");
	static final Datum Z = Symbol.of("z");

	/* (a b c) */
	static Datum proper()
	{
		return new Cons(A, new Cons(B, new Cons(C, Nil.INSTANCE)));
	}

	/* (a b . z) */
	static Datum improper()
	{
		return new Cons(A, new Cons(B, Z));
	}

	/*
	 * ListIterator contract: nextIndex() is the index of the element
	 * next() would return, previousIndex() is nextIndex() - 1.
	 */
	static void assertCursor(int expect, DatumWalk walk)
	{
		assertEquals(expect, walk.nextIndex(), "nextIndex");
		assertEquals(expect - 1, walk.previousIndex(), "previousIndex");
		assertEquals(expect > 0, walk.hasPrevious(), "hasPrevious");
	}

	@Test void testNullOrigin()
	{
		assertThrows(NullPointerException.class, () -> new DatumWalk(null));
	}

	@Test void testFreshIndex()
	{
		assertCursor(0, new DatumWalk(proper()));
		assertCursor(0, new DatumWalk(Nil.INSTANCE));
	}

	@Test void testForward()
	{
		DatumWalk walk = new DatumWalk(proper());
		assertEquals(A, walk.next());
		assertCursor(1, walk);
		assertEquals(B, walk.next());
		assertCursor(2, walk);
		assertEquals(C, walk.next());
		assertCursor(3, walk);
		assertEquals(false, walk.hasNext());
		assertSame(Nil.INSTANCE, walk.rest());
		assertThrows(NoSuchElementException.class, walk::next);
		assertCursor(3, walk);
	}

	@Test void testForwardImproper()
	{
		DatumWalk walk = new DatumWalk(improper());
		assertEquals(A, walk.next());
		assertEquals(B, walk.next());
		assertEquals(false, walk.hasNext());
		assertSame(Z, walk.rest());
		assertCursor(2, walk);
		assertThrows(NoSuchElementException.class, walk::next);
	}

	@Test void testBackward()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.next();
		walk.next();
		walk.next();
		/* at the terminal: previous() must still work */
		assertEquals(C, walk.previous());
		assertCursor(2, walk);
		assertEquals(B, walk.previous());
		assertCursor(1, walk);
		assertEquals(A, walk.previous());
		assertCursor(0, walk);
		assertThrows(NoSuchElementException.class, walk::previous);
		assertCursor(0, walk);
	}

	@Test void testBackwardImproper()
	{
		DatumWalk walk = new DatumWalk(improper());
		walk.next();
		walk.next();
		assertEquals(B, walk.previous());
		assertCursor(1, walk);
	}

	/* alternating next()/previous() returns the same element */
	@Test void testPingPong()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.next();
		assertEquals(B, walk.next());
		assertEquals(B, walk.previous());
		assertEquals(B, walk.next());
		assertCursor(2, walk);
	}

	@Test void testSeekToEnd()
	{
		DatumWalk walk = new DatumWalk(proper());
		assertSame(Nil.INSTANCE, walk.seek(3));
		assertCursor(3, walk);
		assertEquals(false, walk.hasNext());

		walk = new DatumWalk(improper());
		assertSame(Z, walk.seek(2));
		assertCursor(2, walk);
	}

	@Test void testSeekRelative()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.seek(1);
		walk.seek(1);
		assertCursor(2, walk);
		assertEquals(C, walk.next());
		walk.seek(-2);
		assertCursor(1, walk);
		assertEquals(B, walk.next());
	}

	@Test void testSeekZero()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.next();
		walk.seek(0);
		assertCursor(1, walk);
	}

	/* a failed seek should leave the iterator where it was */
	@Test void testSeekPastEnd()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.next();
		assertThrows(IndexOutOfBoundsException.class, () -> walk.seek(5));
		assertCursor(1, walk);
		assertEquals(B, walk.next());
	}

	/* the message reports the absolute index, not the relative offset */
	@Test void testSeekPastEndMessage()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.next();
		String msg = assertThrows(IndexOutOfBoundsException.class,
			() -> walk.seek(5)).getMessage();
		assertTrue(msg.contains("index 6"), msg);
	}

	/* cur + off must not wrap around to a negative index */
	@Test void testSeekOverflow()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.next();
		assertThrows(IndexOutOfBoundsException.class,
			() -> walk.seek(Integer.MAX_VALUE));
		assertCursor(1, walk);
	}

	@Test void testSeekBeforeStart()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.next();
		assertThrows(IndexOutOfBoundsException.class, () -> walk.seek(-2));
		assertCursor(1, walk);
		assertEquals(B, walk.next());
	}

	@Test void testReadOnly()
	{
		DatumWalk walk = new DatumWalk(proper());
		walk.next();
		assertThrows(UnsupportedOperationException.class, walk::remove);
		assertThrows(UnsupportedOperationException.class, () -> walk.set(A));
		assertThrows(UnsupportedOperationException.class, () -> walk.add(A));
	}
}
