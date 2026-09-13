package org.rapidcow.trivnote.libexpr;

/**
 * An atom is just any non-cons non-nil datum.
 * Concretely, there are two types of atoms:
 *
 * <ul>
 *   <li> The {@link Scalar}, which holds an arbitrary value. </li>
 *   <li> The {@link Symbol}, which holds a unique, interned
 *        value from a (hopefully) fixed pool of vocabulary. </li>
 * </ul>
 */
public abstract sealed class Atom
	implements Datum permits Scalar, Symbol
{
	protected final String value;

	protected Atom(String value)
	{
		if (value == null) {
			String className = this.getClass().getSimpleName();
			throw new NullPointerException("value of a " + className + " must not be null");
		}
		this.value = value;
	}

	/**
	 * Return the value.  Presently, an atom's
	 * value is simply a freeform Unicode string.
	 */
	public String value()
	{
		return value;
	}
}
