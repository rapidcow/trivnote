package org.rapidcow.trivnote.libexpr;

public final class Scalar extends Atom
{
	public Scalar(String value)
	{
		super(value);
	}

	@Override public boolean equals(Object o)
	{
		if (this == o) {
			return true;
		}
		if (o instanceof Scalar atom) {
			return value.equals(atom.value);
		}
		return false;
	}

	@Override public int hashCode()
	{
		return value.hashCode();
	}
}
