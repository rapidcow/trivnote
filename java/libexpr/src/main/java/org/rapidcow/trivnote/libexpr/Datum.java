package org.rapidcow.trivnote.libexpr;

/**
 * A datum recursively defines an s-expression.
 * A datum is either a {@link Scalar}, a {@link Cons},
 * or {@link Nil}.
 */
sealed interface Datum permits Scalar, Cons, Nil
{
	/**
	 * Return if this is an atom.  An atom
	 * is any datum that is not a cons cell.
	 */
	default boolean isAtom()
	{
		return !(this instanceof Cons);
	}

	/**
	 * Return if this is a list.  A list is
	 * either a cons cell or nil.  A list is
	 * iterable.
	 */
	default boolean isList()
	{
		return !(this instanceof Scalar);
	}
}
