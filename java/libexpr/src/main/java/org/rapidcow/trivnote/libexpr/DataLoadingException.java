/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/**
 * Parsing exception.
 */
public class DataLoadingException extends RuntimeException
{
	private static final long serialVersionUID = 2823174540633306742L;

	/* Exceptions are serializable, so Javadoc is necessary. */

	/**
	 * Line offset (0-indexed).
	 */
	private final long lineOffset;

	/**
	 * Column offset (0-indexed).
	 */
	private final long charOffset;

	/**
	 * Creates a new parsing exception without chaining.
	 *
	 * @param message    passed to {@link RuntimeException#RuntimeException(String)}
	 * @param lineOffset line offset (0-indexed)
	 * @param charOffset column offset (0-indexed)
	 */
	public DataLoadingException(
		String message,
		long lineOffset,
		long charOffset
	)
	{
		super(message);
		this.lineOffset = lineOffset;
		this.charOffset = charOffset;
	}

	/**
	 * Creates a new parsing exception with chaining.
	 *
	 * @param message   passed to {@link RuntimeException#RuntimeException(String, Throwable)}
	 * @param cause     passed to {@link RuntimeException#RuntimeException(String, Throwable)}
	 * @param lineOffset line offset (0-indexed)
	 * @param charOffset column offset (0-indexed)
	 */
	public DataLoadingException(
		String message,
		Throwable cause,
		long lineOffset,
		long charOffset
	)
	{
		super(message, cause);
		this.lineOffset = lineOffset;
		this.charOffset = charOffset;
	}

	@Override public String toString()
	{
		return ("line " + getLineNumber() +
			" char " + getCharNumber() +
			": " + super.toString());
	}

	/**
	 * {@return the line offset (0-based) of this exception}
	 */
	public long getLineOffset()
	{
		return lineOffset;
	}

	/**
	 * {@return the line number (1-based) of this exception}
	 */
	public long getLineNumber()
	{
		return lineOffset + 1;
	}

	/**
	 * {@return the column number (1-based) of this exception}
	 */
	public long getCharOffset()
	{
		return charOffset;
	}

	/**
	 * {@return the column number (1-based) of this exception}
	 */
	public long getCharNumber()
	{
		return charOffset + 1;
	}
}
