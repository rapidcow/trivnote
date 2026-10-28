/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.util.Objects;

/**
 * The atom devoid of value and the empty list.
 * Note that only {@link Nil#PAREN} is recognized
 * as the empty list and the list terminator; you
 * must extend your notion of list accordingly if
 * you wish to use alternatives.
 */
public
enum
	Nil
implements
	Datum
{
	/**
	 * The singleton that represents {@code nil}.
	 * It is canonically represented by round
	 * parentheses, though others may be used.
	 *
	 * @since %v
	 */
	PAREN(false, '(', ')'),
	/**
	 * {@code nil} with square brackets.
	 * This may be used to represent EDN vectors (arrays).
	 *
	 * @since %v
	 */
	BRACK(false, '[', ']'),
	/**
	 * {@code nil} with curly braces.
	 * This may be used to represent EDN maps (hashes).
	 *
	 * @since %v
	 */
	CURLY(false, '{', '}'),

	/**
	 * Alternative {@code nil} with square brackets.
	 * This may be used to represent Scheme vectors.
	 *
	 * @since %v
	 */
	ALT_PAREN(true, '(', ')'),
	/**
	 * Alternative {@code nil} with square brackets.
	 * It is not known what this may represent.
	 *
	 * @since %v
	 */
	ALT_BRACK(true, '[', ']'),
	/**
	 * Alternative {@code nil} with curly braces.
	 * This may be used to represent Clojure sets.
	 *
	 * @since %v
	 */
	ALT_CURLY(true, '{', '}');

	/**
	 * The singleton that represents {@code nil}.
	 * This is now an alias of {@link #PAREN}.
	 */
	public static final Nil INSTANCE = PAREN;

	private boolean v;
	private int bg;
	private int eg;

	private
	Nil(boolean alt, int bg, int eg)
	{
		this.v = alt;
		this.bg = bg;
		this.eg = eg;
	}

	/**
	 * {@return a string representation of nil}
	 *
	 * @since %v
	 */
	public final String
	value()
	{
		return (v ? "#" : "") + (char) bg + (char) eg;
	}

	/**
	 * {@return a string representation of nil}
	 */
	@Override
	public final String
	toString()
	{
		return value();
	}

	/**
	 * {@return whether this is an alternative form of nil}
	 * Alternative forms start with the {@code #} character.
	 *
	 * @since %v
	 */
	public final boolean
	isAlternative()
	{
		return v;
	}

	/**
	 * {@return opening group delimiter}
	 *
	 * @since %v
	 */
	public final int
	openingGroup()
	{
		return bg;
	}

	/**
	 * {@return closing group delimiter}
	 *
	 * @since %v
	 */
	public final int
	closingGroup()
	{
		return eg;
	}
}
