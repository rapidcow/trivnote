/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * A non-cons non-nil {@link Datum}.
 * Concretely, it covers two types of atoms:
 *
 * <ul>
 *   <li> The {@link Scalar}, which holds an arbitrary value. </li>
 *   <li> The {@link Symbol}, which holds a unique, interned
 *        value from a (hopefully) fixed pool of vocabulary. </li>
 * </ul>
 *
 * <p>
 * Presently, an atom’s value is a freeform Unicode
 * string, represented by {@link #value()}.
 */
public abstract sealed
class
	Atom
implements
	Datum
permits
	Scalar,
	Symbol
{
	/**
	 * Value of this datum.  May not be null.
	 */
	protected final String value;

	/**
	 * Create a new atom with the given value.
	 *
	 * @param value  value
	 * @throws NullPointerException on null {@code value}
	 */
	protected
	Atom(String value)
	{
		if (value == null) {
			String className = getClass().getSimpleName();
			throw new NullPointerException("value of a " + className + " must not be null");
		}
		this.value = value;
	}

	/**
	 * {@return the value of this datum}
	 */
	public String
	value()
	{
		return value;
	}

	/* This mimics a record's toString() override. */
	@Override
	public String
	toString()
	{
		String className = getClass().getSimpleName();
		return className + "[value=" + value + "]";
	}

	/**
	 * Returns whether this equals {@code o}.
	 * Two symbols are considered equal if their values equal.
	 *
	 * @param o  object to compare
	 * @return whether this equals {@code o}
	 */
	public boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}
		/*
		 * A scalar is not equal to a symbol, so
		 * we must check strict class equality.
		 */
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Atom atom = (Atom) o;
		return value.equals(atom.value);
	}

	/**
	 * {@return hash code for this}
	 * Equal atoms hash to the same code.
	 */
	@Override
	public int
	hashCode()
	{
		return value.hashCode();
	}
}
