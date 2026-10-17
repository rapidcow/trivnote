package org.rapidcow.trivnote.libexpr;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final
class
	AssocList
extends
	AbstractMap<Datum,Datum>
{
	private final Datum head;
	private final long size;
	private volatile long pLen = -1;

	public
	AssocList(Datum list)
	{
		DatumWalk iter = new DatumWalk(list);
		while (iter.hasNext()) {
			Datum node = iter.next();
			if (!(node instanceof Cons)) {
				throw new IllegalArgumentException(
					"alist entry is not a cons cell"
				);
			}
		}
		if (iter.rest() != Nil.INSTANCE) {
			throw new IllegalArgumentException(
				"alist is an improper list"
			);
		}
		head = list;
		size = iter.position();
	}

	/* pass-thru */

	@Override
	public Datum
	get(Object key)
	{
		if (!(key instanceof Datum dKey)) {
			return null;
		}
		DatumWalk iter = new DatumWalk(head);
		while (iter.hasNext()) {
			Cons pair = (Cons) iter.next();
			if (pair.car().equals(dKey)) {
				return pair.cdr();
			}
		}
		return null;
	}

	@Override
	public boolean
	containsKey(Object key)
	{
		return get(key) != null;
	}

	@Override
	public final int
	size()
	{
		return (int) Math.min(count(), Integer.MAX_VALUE);
	}

	public final long
	count()
	{
		if (pLen < 0) {
			Set<Datum> uniq = new HashSet<Datum>();
			DatumWalk iter = new DatumWalk(head);
			while (iter.hasNext()) {
				Cons pair = (Cons) iter.next();
				uniq.add(pair.car());
			}
			pLen = uniq.size();
		}
		return pLen;
	}

	public boolean
	hasUniqueKeys()
	{
		return count() == size;
	}

	@Override
	public Set< Map.Entry<Datum,Datum> >
	entrySet()
	{
		throw new UnsupportedOperationException("TODO");
	}
}
