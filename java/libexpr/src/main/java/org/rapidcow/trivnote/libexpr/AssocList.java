package org.rapidcow.trivnote.libexpr;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Associative list view into a datum.
 * Note that this is not a multi-map;
 * search aborts on first pair, like
 * the classic {@code assoc} function.
 *
 * @since 0.1.0
 */
public final
class
	AssocList
extends
	AbstractMap<Datum,Datum>
{
	private final Datum head;
	private final long size;
	/* Unique pair length.  (The name is bad, but eh.) */
	private volatile long pLen = -1;

	/**
	 * Constructs an alist view of the given datum.
	 *
	 * @param list  source alist
	 * @throws NullPointerException on null {@code list}
	 * @throws IllegalArgumentException if the tail
	 *    is not a proper list of cons cells
	 */
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

	/**
	 * Constructs entries from a map.
	 * The map is copied in map entry iteration order.
	 *
	 * @param map  source map
	 * @throws NullPointerException if the map,
	 *    a key, or a value is {@code null}
	 */
	public
	AssocList(Map<? extends Datum, ? extends Datum> map)
	{
		int sz = map.size();
		Deque<Cons> q = new ArrayDeque<>(sz);
		size = sz;
		if (sz == 0) {
			head = Nil.INSTANCE;
			pLen = 0;
			return;
		}
		for (Map.Entry<? extends Datum, ? extends Datum> e : map.entrySet()) {
			Datum car = Objects.requireNonNull(e.getKey(), "map key");
			Datum cdr = Objects.requireNonNull(e.getValue(), "map value");
			q.push(Cons.of(car, cdr));
		}
		head = Datum.ofDeque(q);
	}

	/**
	 * {@return the underlying proper list datum}
	 */
	public final Datum
	head()
	{
		return head;
	}

	/* There is no tail(); an alist's terminator is always Nil.INSTANCE. */

	/**
	 * Finds the value of the first pair with an equal key.
	 *
	 * @param key  key to find
	 * @return the value, or {@code null} if absent
	 */
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

	/**
	 * {@return whether an equal key occurs in this alist}
	 *
	 * @param key  key to find
	 */
	@Override
	public boolean
	containsKey(Object key)
	{
		return get(key) != null;
	}

	/**
	 * {@return the number of distinct keys}
	 * If the length is too great to fit in a signed
	 * 32-bit int, return {@link Integer#MAX_VALUE}.
	 *
	 * @see #count()
	 */
	@Override
	public final int
	size()
	{
		return (int) Math.min(count(), Integer.MAX_VALUE);
	}

	/**
	 * {@return the number of distinct keys}
	 */
	public final long
	count()
	{
		if (pLen < 0) {
			Set<Datum> uniq = new HashSet<>();
			DatumWalk iter = new DatumWalk(head);
			while (iter.hasNext()) {
				Cons pair = (Cons) iter.next();
				uniq.add(pair.car());
			}
			pLen = uniq.size();
		}
		return pLen;
	}

	/**
	 * {@return whether this alist has no entries}
	 */
	@Override
	public final boolean
	isEmpty()
	{
		if (Datum.isAtom(head)) {
			return true;
		}
		return super.isEmpty();
	}

	/**
	 * {@return whether every pair has a distinct key}
	 */
	public boolean
	hasUniqueKeys()
	{
		return count() == size;
	}

	/**
	 * Requires every pair to have a distinct key.
	 *
	 * @return this view
	 * @throws IllegalArgumentException on duplicate keys
	 */
	public AssocList
	requireUniqueKeys()
	{
		if (!hasUniqueKeys()) {
			throw new IllegalArgumentException("Duplicate keys");
		}
		return this;
	}

	private
	AssocList(Datum list, long uniqueSize)
	{
		head = list;
		size = uniqueSize;
		pLen = size;
	}

	/**
	 * Returns a pruned alist of uniquely keyed entries.
	 *
	 * @return this view if keys are already unique,
	 *    otherwise the view of the compacted alist
	 * @throws IllegalStateException if the cached
	 *    distinct-key count exceeds {@link Integer#MAX_VALUE}
	 */
	public AssocList
	withUniqueKeys()
	{
		long llen = pLen;
		int len;
		if (llen == -1) {
			len = -1;
		} else {
			if (llen > Integer.MAX_VALUE) {
				throw new IllegalStateException(
					"Sorry, I won't compact alists whose "
					+ "length exceeds Integer.MAX_VALUE. "
					+ "Please do it yourself with a data "
					+ "structure of your choice."
				);
			}
			len = (int) llen;
		}

		if (len >= 0 && len == size) {
			return this;
		}

		Set<Datum> uniq = new HashSet<>();
		Deque<Cons> q = new ArrayDeque<>(len < 0 ? 1 : len);
		DatumWalk iter = new DatumWalk(head);
		while (iter.hasNext()) {
			Cons pair = (Cons) iter.next();
			if (uniq.add(pair.car())) {
				q.push(pair);
			}
		}
		len = uniq.size();
		pLen = len;
		if (len == size) {
			/*
			 * All keys are unique.  The deque is wasted,
			 * but we won't have to do this for a second
			 * time thanks to caching the pair-length. :)
			 *
			 * Callers with a memory budget may want to
			 * inflict the side-effect of setting pLen
			 * with hasUniqueKeys() before being certain
			 * about redundancy and calling us...
			 */
			return this;
		}
		Datum p = Nil.INSTANCE;
		while (!q.isEmpty()) {
			p = Cons.of(q.pop(), p);
		}
		return new AssocList(p, len);
	}

	/**
	 * {@return a set of entries in this alist}
	 * The entries are read-only as all datums are immutable.
	 */
	@Override
	public Set< Map.Entry<Datum,Datum> >
	entrySet()
	{
		return new EntryView(this);
	}

	private static final
	class
		EntryView
	extends
		AbstractSet< Map.Entry<Datum,Datum> >
	{
		final AssocList me;

		private
		EntryView(AssocList me)
		{
			this.me = me;
		}

		@Override
		public int
		size()
		{
			return me.size();
		}

		@Override
		public Iterator< Map.Entry<Datum,Datum> >
		iterator()
		{
			return new EntryWalk(me);
		}
	}

	private static final
	class
		EntryWalk
	implements
		Iterator< Map.Entry<Datum,Datum> >
	{
		final DatumWalk iter;

		/* Keys already yielded; null when every key is unique */
		final Set<Datum> seen;

		/* First cons of the next unique key, already read from iter */
		Cons pushback;

		private
		EntryWalk(AssocList me)
		{
			iter = new DatumWalk(me.head);
			if (me.hasUniqueKeys()) {
				seen = null;
			} else {
				seen = new HashSet<Datum>();
			}
		}

		/* Advances to the next cons with an unseen key, or null at EOF. */
		private Cons
		nextUnique()
		{
			while (iter.hasNext()) {
				Cons pair = (Cons) iter.next();
				if (seen.add(pair.car())) {
					return pair;
				}
			}
			return null;
		}

		@Override
		public boolean
		hasNext()
		{
			if (seen == null) {
				return iter.hasNext();
			}
			if (pushback == null) {
				pushback = nextUnique();
			}
			return pushback != null;
		}

		@Override
		public Map.Entry<Datum,Datum>
		next()
		{
			Cons p;
			if (seen == null) {
				p = (Cons) iter.next();
			} else {
				if (pushback == null) {
					pushback = nextUnique();
				}
				if (pushback == null) {
					throw new NoSuchElementException();
				}
				/* Hold p, and scan ahead to the next unique key (or EOF) */
				p = pushback;
				pushback = nextUnique();
			}
			return new AbstractMap.SimpleImmutableEntry<>( p.car(), p.cdr() );
		}
	}
}
