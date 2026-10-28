/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.ByteArrayOutputStream;

/**
 * An immutable vector of octets (bytes), written
 * {@code #u8( ... )}.  Like the other leaf types, the backing
 * storage is up to the implementation: extend this class and
 * implement {@link #length()} and {@link #get(int)} if you do
 * not want to keep a {@code byte[]} around.
 *
 * <p>
 * Octets are read and written as unsigned values from 0 to 255.
 * Two octet vectors are equal if and only if they have the same
 * length and the same octets, whatever their class.
 *
 * @since %v
 */
public non-sealed
abstract class
	OctetVector
implements
	Datum
{
	private int hash;

	/**
	 * Default constructor.
	 */
	protected OctetVector()
	{
		/* nothing to do. */
	}

	/**
	 * {@return an immutable octet vector with a copy of the octets}
	 *
	 * @param octets  the given octets
	 * @throws NullPointerException on null {@code octets}
	 * @since %v
	 */
	public static OctetVector
	of(byte... octets)
	{
		return new SimpleOctetVector(octets);
	}

	/**
	 * {@return an immutable octet vector with the bytes written
	 * to the stream so far}  Later writes do not affect the result.
	 *
	 * @param octets  the given stream
	 * @throws NullPointerException on null {@code octets}
	 * @since %v
	 */
	public static OctetVector
	of(ByteArrayOutputStream octets)
	{
		return new SimpleOctetVector(octets);
	}

	/**
	 * {@return the number of octets in this vector}
	 */
	public abstract long
	count();

	/**
	 * {@return the number of octets in this vector}
	 * If the length exceeds the signed 32-bit maximum,
	 * return {@link Integer#MAX_VALUE}.
	 */
	public int
	length()
	{
		return (int) Math.min(count(), Integer.MAX_VALUE);
	}

	/**
	 * {@return the octet at the given index, from 0 to 255}
	 *
	 * @param index  the given index
	 * @throws IndexOutOfBoundsException if {@code index} is
	 *   negative or no less than {@link #count()}
	 */
	public abstract int
	get(long index);

	/**
	 * {@return the octet at the given index, from 0 to 255}
	 *
	 * @param index  the given index
	 * @throws IndexOutOfBoundsException if {@code index} is
	 *   negative or no less than {@link #count()}
	 */
	public int
	get(int index)
	{
		return get((long) index);
	}

	/**
	 * {@return a new array holding the octets of this vector}
	 *
	 * @throws IllegalStateException if the vector is
	 *   too large to fit within a Java array
	 */
	public byte[]
	toByteArray()
	{
		long size = count();
		if (size > Integer.MAX_VALUE) {
			throw new IllegalStateException("too large to fit in byte[]");
		}
		int sz = (int) size;
		byte[] octets = new byte[sz];
		for (int i = 0; i < sz; i++) {
			octets[i] = (byte) get(i);
		}
		return octets;
	}

	/**
	 * {@return a good-enough string representation of this vector}
	 * It does not list the octets.
	 */
	@Override
	public String
	toString()
	{
		return "Octets[length=" + length() + "]";
	}

	/**
	 * Compares this vector against {@code o}.
	 * {@code o} is considered equal if and only if
	 * {@code o} is also an octet vector with the same
	 * octets.
	 *
	 * @param o  the object to compare against
	 * @return whether {@code o} is an equal vector
	 */
	@Override
	public final boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}
		if (o instanceof OctetVector that) {
			long n = count();
			if (n != that.count()) {
				return false;
			}
			for (long i = 0; i < n; i++) {
				if (get(i) != that.get(i)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * {@return hash code for this vector}
	 * This is computed as in {@link java.util.List#hashCode()}
	 * over the unsigned octets, except it never returns 0.
	 * It is nonetheless consistent with {@link #equals(Object)}.
	 */
	@Override
	public final int
	hashCode()
	{
		int h = hash;
		if (h == 0) {
			h = 1;
			long n = count();
			for (long i = 0; i < n; i++) {
				h = 31 * h + get(i);
			}
			if (h == 0) {
				h = 1;
			}
			hash = h;
		}
		return h;
	}
}
