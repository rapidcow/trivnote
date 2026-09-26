package org.rapidcow.trivnote.libexpr;

import java.util.Iterator;
import java.util.Objects;

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
public final class Cons implements Datum, Iterable<Datum>
{
	private final Datum car;
	private final Datum cdr;

	/**
	 * Creates an immutable cons cell.
	 *
	 * @param car  the former half
	 * @param cdr  the latter half
	 * @throws NullPointerException on null {@code car} or {@code cdr}
	 */
	public Cons(Datum car, Datum cdr)
	{
		this.car = Objects.requireNonNull(car, "car must not be null");
		this.cdr = Objects.requireNonNull(cdr, "cdr must not be null");
	}

	/**
	 * {@return the former half}
	 */
	public Datum car()
	{
		return car;
	}

	/**
	 * {@return the latter half}
	 */
	public Datum cdr()
	{
		return cdr;
	}

	/**
	 * {@return a list iterator for this}
	 */
	@Override public Iterator<Datum> iterator()
	{
		return new Walk(this);
	}
}
