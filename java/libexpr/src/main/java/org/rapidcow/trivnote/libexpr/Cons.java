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
 * An immutable pair of {@link Datum}s.
 * A chain of cons cells created by recursively
 * storing cons cells in the cdr forms a list.
 *
 * <p>
 * If the list terminates with a meaningless datum
 * (i.e., nil), it is considered <em>proper</em>.
 * It is otherwise considered <em>improper</em>.
 */
public non-sealed
abstract class
	Cons
implements
	Datum
{
	/**
	 * {@return the car of this cons cell}  The return
	 * value may not be an identical datum object, but
	 * it must be equal.  car may not be null.
	 */
	public abstract Datum
	car();

	/**
	 * {@return the cdr of this cons cell}  The return
	 * value may not be an identical datum object, but
	 * it must be equal.  cdr may not be null.
	 */
	public abstract Datum
	cdr();

	/*
	 * Cached hash.  0 is a served sentinel value.  No thread
	 * locks are acquired when read/writing to this, but this
	 * should be fine as hashing is deterministic.
	 *
	 * Note: If we implement serializable, this field is transient.
	 */
	private int hash;

	/*
	 * Hashing context for the current cons cell.
	 * We keep `p' (stands for "pointer") as a reference to this
	 * cons cell, so that we can publish the cache for future
	 * hashCode() and equals() calls.  `a' stores the accumulated
	 * hash, and `n' counts how many of the two halves have been
	 * folded in (n == 1 means car-only, n == 2 means both).
	 */
	private static final
	class
		HashFrame
	{
		final Cons p;
		int a = 1;
		int n = 0;

		HashFrame(Cons c)
		{
			p = c;
		}

		/**
		 * Update with the hash code of the non-null child datum.
		 *
		 * @param h  hash code
		 */
		void
		fold(int h)
		{
			a = 31 * a + h;
			n++;
		}

		/**
		 * {@return the accumulated hash}  This value is never
		 * 0, because we use 0 to represent uncomputed cache.
		 */
		int
		hash()
		{
			return (a == 0) ? 1 : a;
		}

		/*
		 * Iterate!
		 */
		Datum
		next()
		{
			return ((n == 0)
				? p.car()
				: p.cdr()
			);
		}

		/*
		 * Got s'more?
		 *
		 * Note: this can entirely be spelled as hasNext().
		 * I just find this funnier.  Also I was hungry af.
		 */
		boolean
		sMore()
		{
			return n < 2;
		}
	}

	/**
	 * Returns whether this equals {@code o}.  Two cons
	 * cells are equal if their cars are equal and their
	 * cdrs are equal.
	 *
	 * @implNote This implementation uses a stack to compare
	 *    cons children, instead of recursion which can overflow
	 *    the stack memory fast.  Cached hash codes are used
	 *    to reject peers early, but they are not expected,
	 *    and will not be populated if absent.
	 *
	 * @param o  object to compare
	 * @return whether this equals {@code o}
	 * @since %v
	 */
	@Override
	public final boolean
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
				/* cdr goes under car, so we go car-first. */
				work.push(q.cdr());
				work.push(p.cdr());
				work.push(q.car());
				work.push(p.car());
				continue;
			}
			if (!x.equals(y)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * {@return hash code for this}  Equal cons cells
	 * hash to the same code.
	 *
	 * @since %v
	 * @implNote This implementation uses a stack to
	 *    traverse cons children with an absent hash,
	 *    instead of recursion which can overflow the
	 *    stack memory fast.  The hash is cached; and
	 *    because 0 is used as the sentinel, it never
	 *    returns 0.  (The choice of 0 is arbitrary.)
	 */
	@Override
	public final int
	hashCode()
	{
		/* Race guard */
		int myHash = hash;
		if (myHash != 0) {
			return myHash;
		}

		HashFrame f;
		int h;
		Datum x;

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
			f = work.peek();
			if (!f.sMore()) {
				h = f.hash();
				f.p.hash = h;
				work.pop();
				if (work.isEmpty()) {
					return h;
				}
				work.peek().fold(h);
				continue;
			}
			x = f.next();
			if (x instanceof Cons p) {
				h = p.hash;
				if (h == 0) {
					work.push(new HashFrame(p));
				} else {
					f.fold(h);
				}
				continue;
			}
			f.fold(x.hashCode());
		}
	}

	/**
	 * Creates a non-empty chain with the last datum as its tail.
	 *
	 * @param car  first element
	 * @param cdr  second element
	 * @param etc  optionally more elements
	 * @throws NullPointerException if any argument or
	 *    array element is {@code null}
	 * @return the concatenated cons chain
	 * @since %v
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
	 * Creates a non-empty cons chain with the given tail.
	 *
	 * @param car  first element
	 * @param etc  rest elements
	 * @param tail  list terminator
	 * @throws NullPointerException if any argument or
	 *    array element is {@code null}
	 * @return the concatenated cons chain
	 * @since %v
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
	 * @param car  first element
	 * @param etc  rest elements
	 *
	 * @throws NullPointerException if any argument or array element is null
	 * @return the concatenated cons chain
	 * @since %v
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
	 * @param list  list of origin
	 * @throws IllegalArgumentException if {@code list} is empty
	 * @throws NullPointerException if {@code list} or any element is null
	 * @return the transformed cons chain
	 * @since %v
	 */
	public static ConsList
	fromList(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Empty list");
		}
		return new ConsList(list);
	}
}
