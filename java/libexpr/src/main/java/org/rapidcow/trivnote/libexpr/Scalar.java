package org.rapidcow.trivnote.libexpr;

public sealed class Scalar
	implements Datum permits Symbol
{
	/* String value. */
	final String value;

	public Scalar(String value)
	{
		if (value == null) {
			String className = this.getClass().getSimpleName();
			throw new NullPointerException("value of a " + className + " must not be null");
		}
		this.value = value;
	}

	public String value()
	{
		return value;
	}

	@Override public boolean equals(Object that)
	{
		if (this == that) {
			return true;
		}
		if (that instanceof Scalar atom) {
			return this.value.equals(atom.value);
		}
		return false;
	}
}
