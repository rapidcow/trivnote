package org.rapidcow.trivnote.libexpr;

/**
 * Numeric scalar exactness.
 *
 * @since 0.1.0
 */
public enum Exactness
{
	/**
	 * Exactness was not specified.
	 */
	UNSPECIFIED,
	/**
	 * Exact.  Corresponds to the prefix <q>{@code %e}</q>.
	 */
	EXACT,
	/**
	 * Inexact.  Corresponds to the prefix <q>{@code %i}</q>.
	 */
	LOSSY;
}
