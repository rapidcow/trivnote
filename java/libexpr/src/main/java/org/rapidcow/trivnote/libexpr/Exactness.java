package org.rapidcow.trivnote.libexpr;

/**
 * Numeric scalar exactness.
 *
 * @since %v
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
