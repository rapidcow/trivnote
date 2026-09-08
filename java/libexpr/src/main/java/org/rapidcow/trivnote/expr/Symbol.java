package org.rapidcow.trivnote.expr;

sealed interface Symbol
	permits Symbol.Atom, Symbol.Cons
{
	boolean isAtom();

	final class Atom implements Symbol
	{
		@Override public boolean isAtom()
		{
			return true;
		}
	}

	final class Cons implements Symbol
	{
		@Override public boolean isAtom()
		{
			return false;
		}
	}
}
