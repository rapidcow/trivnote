/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A pair of {@link Datum}s.
 * A chain of cons cells created by recursively
 * appending to cdr forms a list.
 *
 * <p>
 * If the list terminates with a meaningless datum
 * (i.e., nil), it is considered <em>proper</em>.
 * It is otherwise considered <em>improper</em>.
 * Cons cells are {@link Iterable} as a list.
 */
public non-sealed
abstract class
	Cons
implements
	Datum
{
	public abstract Datum car();
	public abstract Datum cdr();

	/*
	 * Cached structural hash; 0 means "not computed yet".  A hash
	 * that happens to work out to 0 is stored as 1 instead, so the
	 * cache never misses forever.  Racy but benign, like String's:
	 * every thread computes the same value for an immutable cell.
	 *
	 * Note: If we implement serializable, this field is transient.
	 */
	private int hash;

	/*
	 * One cell whose hash is being computed.  The hash folds in
	 * the car's hash and then the cdr's, as Objects.hash(car, cdr)
	 * does; `n' counts how many of the two are folded in so far.
	 * Each of car() and cdr() is called once per frame, so a Cons
	 * that makes a fresh view on every call is still fine.
	 * (Header, one reference and two ints: 24 bytes with compressed
	 * pointers, so nothing is lost to padding.)
	 */
	private static final
	class
		HashFrame
	{
		final Cons p;
		int a = 1;
		int n;

		HashFrame(Cons c)
		{
			p = c;
		}

		void
		fold(int h)
		{
			a = 31 * a + h;
			n++;
		}

		int
		hash()
		{
			return (a == 0) ? 1 : a;
		}

		Datum
		next()
		{
			return ((n == 0)
				? p.car()
				: p.cdr()
			);
		}

		boolean
		sMore()
		{
			return n < 2;
		}
	}

	/**
	 * Returns whether this equals {@code o}.  Two conses are equal
	 * if their cars are equal and their cdrs are equal, regardless
	 * of their concrete classes.
	 *
	 * @implNote The comparison walks both trees with an explicit
	 *    stack, so neither long lists nor deep nesting can overflow
	 *    the call stack.  Pairs of conses that both already cached
	 *    their hash codes are told apart by those codes alone when
	 *    the codes differ.
	 *
	 * @param o  object to compare
	 * @return whether this equals {@code o}
	 */
	@Override
	public boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}

		if (!(o instanceof Cons that)) {
			return false;
		}

		Deque<Datum> work = new ArrayDeque<>();
		work.push(that);
		work.push(this);
		while (!work.isEmpty()) {
			Datum x = work.pop();
			Datum y = work.pop();
			if (x == y) {
				continue;
			}
			if (x instanceof Cons p) {
				if (!(y instanceof Cons q)) {
					return false;
				}
				/* Race guard */
				int pH = p.hash;
				int qH = q.hash;
				if (pH != 0 && qH != 0 && pH != qH) {
					return false;
				}
				/* cdr goes under car, so we go car-first */
				work.push(q.cdr());
				work.push(p.cdr());
				work.push(q.car());
				work.push(p.car());
			}
			else if (!x.equals(y)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * {@return hash code for this}  Equal conses hash to
	 * the same code.  The code is computed once and cached.
	 *
	 * @implNote Conses below this one that have not cached
	 *    their codes yet get them computed and cached along the
	 *    way, using an explicit stack rather than recursion.
	 */
	@Override
	public int
	hashCode()
	{
		/* Race guard */
		int myHash = hash;
		if (myHash != 0) {
			return myHash;
		}

		/*
		 * Post-order walk.  A finished frame hands its hash to the
		 * frame below it (its parent) instead of the parent going
		 * back to read it from the field, so every racy field is
		 * read at most once per visit, and a stale 0 only means we
		 * compute that cell ourselves.  Each frame folds exactly
		 * two values in, so the walk always ends.
		 */
		Deque<HashFrame> work = new ArrayDeque<>();
		work.push(new HashFrame(this));
		for (;;) {
			HashFrame f = work.peek();
			if (f.sMore()) {
				Datum cxr = f.next();
				if (cxr instanceof Cons p) {
					int h = p.hash;
					if (h == 0) {
						work.push(new HashFrame(p));
					} else {
						f.fold(h);
					}
				} else {
					f.fold(cxr.hashCode());
				}
			} else {
				int h = f.hash();
				f.p.hash = h;
				work.pop();
				if (work.isEmpty()) {
					return h;
				}
				work.peek().fold(h);
			}
		}
	}

	/**
	 * Creates a non-empty chain with the last datum as its tail.
	 *
	 * @throws NullPointerException if any argument or array element is null
	 */
	public static Cons
	of(Datum car, Datum cdr, Datum... etc)
	{
		if (etc.length == 0) {
			return new ConsCell(car, cdr);
		}
		return new ConsList(car, cdr, etc);
	}

	/**
	 * Creates a non-empty chain with the given tail.
	 *
	 * @throws NullPointerException if any argument or array element is null
	 */
	public static Cons
	of(Datum car, Datum[] etc, Datum tail)
	{
		if (etc.length == 0) {
			return new ConsCell(car, tail);
		}
		return new ConsList(car, etc, tail);
	}

	/**
	 * Creates a non-empty proper list.
	 *
	 * @throws NullPointerException if any argument or array element is null
	 */
	public static Cons
	ofList(Datum car, Datum... etc)
	{
		if (etc.length == 0) {
			return new ConsCell(car, Nil.INSTANCE);
		}
		return new ConsList(car, etc, Nil.INSTANCE);
	}

	/**
	 * Creates a cons from a non-empty list.  If list is
	 * possibly empty, use {@link Datum#fromList(List)}.
	 *
	 * @throws IllegalArgumentException if {@code list} is empty
	 * @throws NullPointerException if {@code list} or any element is null
	 */
	public static Cons
	fromList(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Empty list");
		}
		return new ConsList(list);
	}
}
