/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * A cons cell that is also the head of a vector, written
 * {@code #( ... )}.  Its cdr chain is terminated by
 * {@link Nil#ALT_PAREN} rather than nil, and the elements
 * can be reached by index without walking the chain.  Like
 * {@link OctetVector}, the backing storage is up to the
 * implementation: extend this class and implement
 * {@link #count()}, {@link #get(long)} and {@link #cdr()}.
 *
 * <p>
 * A cons vector is never empty, since the empty vector is
 * {@link Nil#ALT_PAREN}.  Indices and counts are relative to
 * this cell, so the cdr is the vector without its first element.
 * Use {@link DatumVector} to treat either of the two as one type.
 *
 * @since %v
 */
public
abstract class
	ConsVector
extends
	Cons
{
	/**
	 * Default constructor.
	 */
	protected ConsVector()
	{
		/* nothing to do. */
	}

	/**
	 * Creates a vector of one element.
	 *
	 * @param car  the element
	 * @throws NullPointerException on null {@code car}
	 * @return a vector of one element
	 */
	public static ConsVector
	of(Datum car)
	{
		return of(car, new Datum[0]);
	}

	/**
	 * Creates a vector of two elements.  This hides
	 * {@link Cons#of(Datum, Datum)}, which would make
	 * a pair instead.
	 *
	 * @param car  first element
	 * @param cdr  second element
	 * @throws NullPointerException on null {@code car}
	 *    or {@code cdr}
	 * @return a vector of two elements
	 */
	public static ConsVector
	of(Datum car, Datum cdr)
	{
		return of(car, new Datum[] { cdr });
	}

	/**
	 * Creates a vector of the given elements.  This hides
	 * {@link Cons#of(Datum, Datum, Datum...)}, whose last
	 * datum is a tail instead of an element.
	 *
	 * @param car  first element
	 * @param cdr  second element
	 * @param etc  optionally more elements
	 * @throws NullPointerException if any argument or
	 *    array element is {@code null}
	 * @return a vector of the given elements
	 */
	public static ConsVector
	of(Datum car, Datum cdr, Datum... etc)
	{
		Objects.requireNonNull(etc, "etc");
		Datum[] rest = new Datum[1 + etc.length];
		rest[0] = cdr;
		System.arraycopy(etc, 0, rest, 1, etc.length);
		return of(car, rest);
	}

	private static ConsVector
	of(Datum car, Datum[] etc)
	{
		List<Datum> vec = new ArrayList<>(1 + etc.length);
		vec.add(Objects.requireNonNull(car, "car"));
		for (Datum d : etc) {
			vec.add(Objects.requireNonNull(d, "etc"));
		}
		return new ArrayConsVector(vec, 0);
	}

	/**
	 * Prefixes a one-element vector with a deque.
	 * Elements are popped from the back and appended
	 * one at a time, followed by the element of {@code p}.
	 * This drains the deque and is thus destructive!
	 *
	 * @param q  the deque
	 * @param p  base cons chain, which must be a vector of
	 *    exactly one element (its cdr is {@link Nil#ALT_PAREN})
	 * @return the concatenated vector
	 * @throws NullPointerException if any argument or
	 *    deque element is {@code null}
	 * @throws IllegalArgumentException if the terminator
	 *    of {@code p} is not {@link Nil#ALT_PAREN}
	 */
	public static ConsVector
	of(Deque<? extends Datum> q, Cons p)
	{
		Objects.requireNonNull(q, "q");
		Objects.requireNonNull(p, "p");
		return new ArrayConsVector(q, p);
	}

	/**
	 * {@return the number of elements from this cell onward}
	 * This is at least 1.
	 */
	public abstract long
	count();

	/**
	 * {@return the number of elements from this cell onward}
	 * If the count exceeds the signed 32-bit maximum,
	 * return {@link Integer#MAX_VALUE}.
	 */
	public int
	length()
	{
		return (int) Math.min(count(), Integer.MAX_VALUE);
	}

	/**
	 * {@return the element at the given index}
	 *
	 * @param index  the given index
	 * @throws IndexOutOfBoundsException if {@code index} is
	 *   negative or no less than {@link #count()}
	 */
	public abstract Datum
	get(long index);

	/**
	 * {@return the element at the given index}
	 *
	 * @param index  the given index
	 * @throws IndexOutOfBoundsException if {@code index} is
	 *   negative or no less than {@link #count()}
	 */
	public Datum
	get(int index)
	{
		return get((long) index);
	}

	/**
	 * {@return the first element, same as {@code get(0)}}
	 */
	@Override
	public final Datum
	car()
	{
		return get(0L);
	}

	/**
	 * {@return {@link Nil#ALT_PAREN}}
	 */
	@Override
	public final Nil
	delimiter()
	{
		return Nil.ALT_PAREN;
	}
}
