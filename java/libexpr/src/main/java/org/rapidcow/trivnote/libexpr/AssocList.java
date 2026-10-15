package org.rapidcow.trivnote.libexpr;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public
class
	AssocList
extends
	AbstractMap<String,Datum>
{
	/* Hash of atom value to its first occurrence */
	private HashMap<String,Datum> hash;

	public AssocList(Datum list)
	{
		hash = new HashMap<String,Datum>();

		DatumWalk iter = new DatumWalk(list);
		while (iter.hasNext()) {
			Datum node = iter.next();

			Datum aK, aV;
			if (node instanceof Cons pair) {
				aK = pair.car();
				aV = pair.cdr();
			} else {
				throw new IllegalArgumentException(
					"alist entry is not a cons cell"
				);
			}

			String sK;
			if (aK instanceof Atom atom) {
				sK = atom.value();
			} else {
				throw new IllegalArgumentException(
					"alist entry car is not a non-nil atom"
				);
			}

			if (!hash.containsKey(sK)) {
				hash.put(sK, aV);
			}
		}

		if (iter.rest() != Nil.INSTANCE) {
			throw new IllegalArgumentException(
				"alist is an improper list"
			);
		}
	}

	@Override
	public Set< Map.Entry<String,Datum> >
	entrySet() {
		/* TODO still stub!  This is inefficient. */
		return Collections.unmodifiableMap(hash).entrySet();
	}

}
