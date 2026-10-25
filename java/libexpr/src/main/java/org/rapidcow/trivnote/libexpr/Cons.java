/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
extends
	Datum
{
	/**
	 * {@return the car of this cons cell}  The return
	 * value may not be an identical datum object, but
	 * it must be equal.  car may not be {@code null}.
	 */
	public abstract Datum
	car();

	/**
	 * {@return the cdr of this cons cell}  The return
	 * value may not be an identical datum object, but
	 * it must be equal.  cdr may not be {@code null}.
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
	 * @since 0.1.0
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
	 * @since 0.1.0
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

	/*
	 * These are not only handy shorthands, but they
	 * also pick the optimal implementation! :o
	 *
	 * Namely, any proper list uses a ConsTail instead,
	 * and binary lists (such as the symbol-string pair
	 * in a Racket xexpr attribute) do not have their
	 * length stored.
	 */

	/**
	 * Creates a cons cell.
	 *
	 * @param car  the car
	 * @param cdr  the cdr
	 * @throws NullPointerException on null {@code car}
	 *    or {@code cdr}
	 * @return a cons cell
	 * @since 0.1.0
	 */
	public static Cons
	of(Datum car, Datum cdr)
	{
		return (cdr == Nil.INSTANCE)
			? new ConsTail(car)
			: new ConsCell(car, cdr);
	}

	/**
	 * Creates a non-empty cons chain with the last datum as its tail.
	 *
	 * @param car  first element
	 * @param cdr  second element
	 * @param etc  optionally more elements
	 * @throws NullPointerException if any argument or
	 *    array element is {@code null}
	 * @return the concatenated cons chain
	 * @since 0.1.0
	 */
	public static Cons
	of(Datum car, Datum cdr, Datum... etc)
	{
		if (etc.length == 0) {
			return Cons.of(car, cdr);
		}
		if (etc.length == 1 && etc[0] == Nil.INSTANCE) {
			return Cons.ofList(car, cdr);
		}
		return new ConsList(car, cdr, etc);
	}

	/**
	 * Creates a non-empty cons chain with the given tail.
	 *
	 * @param car  first element
	 * @param etc  the rest
	 * @param tail list terminator
	 * @throws NullPointerException if any argument or
	 *    array element is {@code null}
	 * @return the concatenated cons chain
	 * @since 0.1.0
	 */
	public static Cons
	of(Datum car, Datum[] etc, Datum tail)
	{
		if (etc.length == 0) {
			return Cons.of(car, tail);
		}
		if (etc.length == 1 && tail == Nil.INSTANCE) {
			return Cons.ofList(car, etc[0]);
		}
		return new ConsList(car, etc, tail);
	}

	/**
	 * Prepends a deque to a cons chain.
	 * Elements are popped from the front and prepended
	 * one at a time, reversing their iteration order.
	 * This drains the deque and is thus destructive!
	 *
	 * @param q  the deque
	 * @param p  the cons cell
	 * @return the concatenated cons chain
	 * @throws NullPointerException if any argument or
	 *    deque element is {@code null}
	 * @since 0.1.0
	 */
	public static Cons
	of(Deque<? extends Datum> q, Cons p)
	{
		Objects.requireNonNull(q, "q");
		Objects.requireNonNull(p, "p");
		if (q.isEmpty()) {
			return p;
		}
		if (p.cdr() == Nil.INSTANCE && q.size() == 1) {
			return new ConsCell(q.pop(), p);
		}
		return new ConsList(q, p);
	}

	/**
	 * Creates a proper list of one element.
	 *
	 * @param first  first element
	 * @throws NullPointerException on null {@code first}
	 * @return a nil-terminated singleton
	 * @since 0.1.0
	 */
	public static Cons
	ofList(Datum first)
	{
		return new ConsTail(first);
	}

	/**
	 * Creates a proper list of two elements.
	 *
	 * @param first  first element
	 * @param second second element
	 * @throws NullPointerException on null {@code first}
	 *    or {@code second}
	 * @return a nil-terminated two-element list
	 * @since 0.1.0
	 */
	public static Cons
	ofList(Datum first, Datum second)
	{
		return new ConsCell(first, new ConsTail(second));
	}

	/**
	 * Creates a non-empty proper list.
	 *
	 * @param first  first element
	 * @param etc  the rest
	 * @throws NullPointerException if any argument or
	 *    array element is {@code null}
	 * @return the concatenated cons chain
	 * @since 0.1.0
	 */
	public static Cons
	ofList(Datum first, Datum... etc)
	{
		if (etc.length == 0) {
			return Cons.ofList(first);
		}
		if (etc.length == 1) {
			return Cons.ofList(first, etc[0]);
		}
		return new ConsList(first, etc, Nil.INSTANCE);
	}

	/**
	 * Creates a cons from a non-empty list.  If list is
	 * possibly empty, use {@link Datum#fromList(List)}.
	 *
	 * @param list  list of origin
	 * @throws IllegalArgumentException on an empty
	 *    {@code list}
	 * @throws NullPointerException if {@code list} or
	 *    any element is {@code null}
	 * @return the transformed cons chain
	 * @since 0.1.0
	 */
	public static Cons
	fromList(List<? extends Datum> list)
	{
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Empty list");
		}
		int size = list.size();
		if (size == 1) {
			return Cons.ofList(list.get(0));
		}
		if (size == 2) {
			Iterator<? extends Datum> walk = list.iterator();
			Datum a = walk.next();
			Datum b = walk.next();
			return Cons.ofList(a, b);
		}
		return new ConsList(list);
	}
}
