package org.rapidcow.trivnote.libexpr;

/**
 * Basic unit of an S-expression.
 * A datum is either an {@link Atom}, a {@link Cons},
 * or {@link Nil}.
 *
 * <p>
 * Note that {@link Nil} is both an “atom” (to be
 * distinguished from the {@link Atom} class) and
 * a “list”.
 */
public sealed interface Datum
	permits Atom, Cons, Nil
{
	/**
	 * {@return whether this is an atom}  An atom
	 * is any datum that is not a cons cell.
	 */
	default boolean isAtom()
	{
		return !(this instanceof Cons);
	}

	/**
	 * {@return whether this is a list}  A list
	 * is either a cons cell or nil.  A list is
	 * iterable, though you must cast it to the
	 * specific class to please the compiler.
	 */
	default boolean isList()
	{
		return !(this instanceof Atom);
	}
}
