/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

/**
 * A simple octet vector is only backed by a native Java byte array.
 */
final
class
	SimpleOctetVector
extends
	OctetVector
{
	private final byte[] octets;

	/**
	 * Constructs a new octet vector from a copy of the array.
	 *
	 * @param octets  the given octets
	 */
	public
	SimpleOctetVector(byte[] octets)
	{
		this.octets = octets.clone();
	}

	/**
	 * Constructs a new octet vector from the bytes written to
	 * the stream so far.
	 *
	 * @param octets  the given stream
	 */
	public
	SimpleOctetVector(ByteArrayOutputStream octets)
	{
		this.octets = Objects.requireNonNull(octets).toByteArray();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int
	length()
	{
		return octets.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int
	get(int index)
	{
		return octets[index] & 0xff;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final byte[]
	toByteArray()
	{
		return octets.clone();
	}
}
