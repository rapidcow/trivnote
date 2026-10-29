package org.rapidcow.trivnote.libexpr;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final
class
	ConsVector
extends
	Cons
{
	private final List<Datum> vec;
	private final int off;

	ConsVector(List<Datum> vec, int off)
	{
		this.vec = vec;
		this.off = off;
	}

	public
	ConsVector(Deque<Datum> q, Cons p)
	{
		if (p.cdr() != Nil.ALT_PAREN) {
			throw new IllegalArgumentException(
				"Terminator is not #()"
			);
		}
		List<Datum> vec = new ArrayList<>();
		while (!q.isEmpty()) {
			vec.add(q.removeLast());
		}
		vec.add(p.car());
		this.vec = vec;
		this.off = 0;
	}

	@Override
	public final Nil
	delimiter()
	{
		return Nil.ALT_PAREN;
	}

	@Override
	public final Datum
	car()
	{
		return vec.get(off);
	}

	@Override
	public final Datum
	cdr()
	{
		int size = vec.size();
		int off1 = 1 + off;
		return off1 < size ? new ConsVector(vec, off1) : Nil.ALT_PAREN;
	}
}
