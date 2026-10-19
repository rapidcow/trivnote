/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * S-expression reader.
 */
public
class
	DataLoader
implements
	Iterable<Datum>,
	Iterator<Datum>
{
	/**
	 * Input character stream.
	 */
	private final PushbackReader in;

	/**
	 * Symbol cache.
	 */
	private Map<String,Symbol> sym;

	/**
	 * Most levels of list nesting, and most elements per list.
	 */
	private long maxDepth = Long.MAX_VALUE;
	private long maxLength = Long.MAX_VALUE;

	private boolean fini = false;
	private IOException eieio = null;
	private Datum st = null;

	private long row = 0;
	private long col = 0;
	private long ocl = 0;

	/**
	 * Creates a reader from a character stream.
	 *
	 * @param file  input character stream
	 */
	public
	DataLoader(Reader file)
	{
		this.in = new PushbackReader(file);
	}

	/**
	 * Creates a reader from a byte stream.
	 *
	 * @param file    input byte stream
	 * @param charset character encoding
	 */
	public
	DataLoader(InputStream file, Charset charset)
	{
		this(new InputStreamReader(file, charset));
	}

	/**
	 * Creates a reader from a byte stream with the default
	 * character encoding, {@link StandardCharsets#UTF_8}.
	 *
	 * @param file  input byte stream
	 */
	public
	DataLoader(InputStream file)
	{
		this(file, StandardCharsets.UTF_8);
	}

	/**
	 * Use a map for symbol lookup upon construction.
	 * If {@code null}, remove the cache.
	 *
	 * <p>
	 * Symbols not yet in the map are added to it, so the map grows
	 * as new vocabulary is read.  If the map is read-only (such as
	 * one made by {@link Map#of}), known symbols still resolve to the
	 * instances it holds, and unknown symbols are created without
	 * being remembered.
	 *
	 * <p>
	 * The loader does not synchronize on the map.  If it is shared
	 * between loaders running on different threads, it must be safe
	 * for concurrent use, such as a {@link java.util.concurrent.ConcurrentHashMap}.
	 *
	 * @param sym  map of symbols, optionally
	 *    writable if the map is allowed to grow
	 * @return this
	 * @since %v
	 */
	public final DataLoader
	useSymbolCache(Map<String,Symbol> sym)
	{
		this.sym = sym;
		return this;
	}

	/**
	 * {@return the current symbol cache}
	 * @since %v
	 */
	public final Optional< Map<String,Symbol> >
	getSymbolCache()
	{
		return Optional.ofNullable(sym);
	}

	/**
	 * Limit how deeply lists may nest.  A top-level atom has depth
	 * 0, and each enclosing list adds 1, so {@code (a (b))} needs a
	 * limit of at least 2.  A limit of 0 allows no lists at all.
	 * Exceeding the limit is a {@link DataLoadingException}.
	 *
	 * <p>
	 * The loader recurses once per level, so a deep enough input
	 * can still exhaust the call stack below a high limit; set one
	 * when the input is not trusted.  There is no limit by default.
	 *
	 * @param depth  the most levels of nesting allowed
	 * @return this
	 * @throws IllegalArgumentException if {@code depth} is negative
	 * @since %v
	 */
	public final DataLoader
	useDepthLimit(long depth)
	{
		if (depth < 0) {
			throw new IllegalArgumentException("Negative depth");
		}
		maxDepth = depth;
		return this;
	}

	/**
	 * {@return the most levels of list nesting allowed}
	 * @since %v
	 */
	public final long
	getDepthLimit()
	{
		return maxDepth;
	}

	/**
	 * Limit how many elements a single list may have.  The tail
	 * after a dot is not an element, so {@code (a b . c)} has two.
	 * Exceeding the limit is a {@link DataLoadingException}.
	 * There is no limit by default.
	 *
	 * @param length  the most elements per list allowed
	 * @return this
	 * @throws IllegalArgumentException if {@code length} is negative
	 * @since %v
	 */
	public final DataLoader
	useLengthLimit(long length)
	{
		if (length < 0) {
			throw new IllegalArgumentException("Negative length");
		}
		maxLength = length;
		return this;
	}

	/**
	 * {@return the most elements per list allowed}
	 * @since %v
	 */
	public final long
	getLengthLimit()
	{
		return maxLength;
	}

	private
	static class
		Token
	{
		final Datum value;
		final boolean dot;

		Token(Datum v)
		{
			this(v, false);
		}

		Token(Datum v, boolean o)
		{
			value = v;
			dot = o;
		}

		static final Token DOT = new Token(null, true);
	}

	private static boolean
	isPOSIXspace(int v)
	{
		return (
			v == ' '    /* 0x20  SP */
			|| v == '\n'   /* 0x0A  LF */
			|| v == '\t'   /* 0x09  HT */
			|| v == '\r'   /* 0x0D  CR */
			|| v == '\013' /* 0x0B  VT */
			|| v == '\f'   /* 0x0C  FF */
		);
	}

	private static boolean
	isPOSIXdigit(int v)
	{
		return '0' <= v && v <= '9';
	}

	/**
	 * Reads one character from the underlying reader.
	 * This advances the cursor position used by
	 * {@link #loadingException(String)}.
	 *
	 * @return the character read, or -1 on EOF
	 * @throws IOException on I/O error
	 */
	protected final int
	readChar()
	throws
		IOException
	{
		int v = in.read();
		if (v != -1) {
			ocl = col;
			if (v == '\n') {
				row++;
				col = 0;
			}
			else {
				col++;
			}
		}
		return v;
	}

	/**
	 * Pushes back a character.  This also restores the cursor
	 * position to where it was before that character was read.
	 * Only the most recent call to {@link #readChar()} can be
	 * undone this way, and only once; the position is not restored
	 * correctly if this is called twice in a row.
	 *
	 * @param v  the character most recently returned by {@link #readChar()}
	 * @throws IOException on I/O error
	 */
	protected final void
	unreadChar(int v)
	throws
		IOException
	{
		in.unread(v);
		if (v == '\n') {
			row--;
		}
		col = ocl;
	}

	/**
	 * {@return this}
	 */
	@Override
	public final Iterator<Datum>
	iterator()
	{
		return this;
	}

	/**
	 * {@return whether no I/O error occurred, and any datum remains}
	 * If false, caller should check {@link #lastIOException()} to
	 * tell I/O exceptions from EOF.
	 */
	@Override
	public boolean
	hasNext()
	{
		if (fini) {
			return false;
		}
		if (st == null) {
			try {
				st = readDatum();
			} catch (IOException e) {
				eieio = e;
				fini = true;
			}
		}
		return st != null;
	}

	/**
	 * {@return the next datum in the stream}
	 * Caller should check {@link #lastIOException()}
	 * to tell I/O exceptions apart from EOF.
	 *
	 * @throws NoSuchElementException on I/O error, or EOF
	 * @throws DataLoadingException on syntax error
	 */
	@Override
	public Datum
	next()
	{
		Datum s;
		if (hasNext()) {
			s = st;
			st = null;
			return s;
		}
		throw new NoSuchElementException(eieio);
	}

	/**
	 * {@return the most recent I/O error}
	 * Caller is responsible to check this to
	 * tell EOF from I/O error.
	 */
	public final IOException
	lastIOException()
	{
		return eieio;
	}

	/**
	 * Hook called when a symbol token has been read.
	 * Subclasses may override this to validate or transform
	 * symbols; throw the result of loadingException to reject one.
	 *
	 * @param value  the text of the symbol
	 * @return a symbol for value, from the symbol cache if any
	 * @since %v
	 */
	protected Symbol
	makeSymbol(String value)
	{
		if (sym == null) {
			return Symbol.of(value);
		}
		/*
		 * Make Map::get (cache hit) the fast route:
		 * we anticipate that we will encounter more
		 * recurring symbols than we come across new
		 * ones.  It is also free of side effects,
		 * which means maps like ConcurrentHashMap don't
		 * even need to acquire a lock!  In any case,
		 * it's worth trying....
		 */
		Symbol v = sym.get(value);
		if (v != null) {
			return v;
		}
		try {
			return sym.computeIfAbsent(value, Symbol::of);
		} catch (UnsupportedOperationException e) {
			return Symbol.of(value);
		}
	}

	/**
	 * Hook called when a quoted string token has been read.
	 * The cursor is already past the closing quote, so a position
	 * reported from here points just after the token.
	 *
	 * @param value  the text of the string, with escapes processed
	 * @return a string scalar by default
	 * @since %v
	 */
	protected Scalar
	makeString(String value)
	{
		return Scalar.ofString(value);
	}

	/**
	 * Hook called when a number-like token has been read.
	 * The loader only recognizes the shape of a number and does
	 * not convert it, so value is the token text as written.
	 * The cursor is already past the token, so a position
	 * reported from here points just after it.
	 *
	 * @param value  the text of the number
	 * @return a plain scalar by default
	 * @since %v
	 */
	protected Scalar
	makeSimple(String value)
	{
		return Scalar.of(value);
	}

	/**
	 * Hook called to create a non-empty list.
	 *
	 * @param q  list contents
	 * @param s  final list datum
	 * @param e  list terminator
	 * @return a list
	 */
	protected Cons
	makeList(Deque<Datum> q, Datum s, Datum e)
	{
		return new ConsList(q, s, e);
	}

	/**
	 * {@return a new exception carrying the current cursor position}
	 *
	 * @param message  the detail message
	 * @since %v
	 */
	protected final DataLoadingException
	loadingException(String message)
	{
		DataLoadingException e
			= new DataLoadingException(message, row, col);
		StackTraceElement[] f = e.getStackTrace();
		/* Skip this frame */
		e.setStackTrace(Arrays.copyOfRange(f, 1, f.length));
		return e;
	}

	/**
	 * Reads and returns the next datum.
	 *
	 * @return the next datum, or {@code null} on EOF
	 * @throws IOException on {@link Reader#read} error
	 */
	public Datum
	readDatum()
	throws
		IOException
	{
		Token tk = readDatum(-1, 0);
		if (tk == null) {
			return null;
		}
		if (tk.dot) {
			throw loadingException("Unexpected `.'");
		}
		return tk.value;
	}

	/*
	 * bg is the opening bracket of the list being read (-1 if none),
	 * and depth is how many lists enclose the datum being read.
	 */
	private Token
	readDatum(int bg, long depth)
	throws
		IOException
	{
		int v, w, eg, sg;
		Datum e;
		StringBuilder sb;

		switch (bg) {
		case -1:
			eg = -1;
			break;
		case '(':
			eg = ')';
			break;
		case '[':
			eg = ']';
			break;
		case '{':
			eg = '}';
			break;
		default:
			throw new RuntimeException("unreachable");
		}

		while (( v = readChar() ) != -1) {
			if (isPOSIXspace(v)) {
				continue;
			}

			if (v == eg) {
				return null;
			}

			if (v == '.') {
				if (( w = readChar() ) == -1) {
					return Token.DOT;
				}
				else {
					if (isPOSIXspace(w)) {
						return Token.DOT;
					}
					else if (
						w == '(' || w == '[' || w == '{' ||
						w == ')' || w == ']' || w == '}' ||
						w == '"' || w == '|' || w == ';'
					) {
						unreadChar(w);
						return Token.DOT;
					}
					else if (isPOSIXdigit(w)) {
						sb = new StringBuilder()
							.append((char) v)
							.append((char) w);
						if (( w = readPlain(sb) ) != -1) {
							unreadChar(w);
						}
						return new Token(makeSimple(sb.toString()));
					}
					unreadChar(w);
				}
			}

			if (v == ';') {
				while (( v = readChar() ) != -1) {
					if (v == '\n') {
						break;
					}
				}
				continue;
			}

			if (v == '#') {
				if (( w = readChar() ) != -1) {
					if (w == '|') {
						skipBlockComment();
						continue;
					}
					if (w == ';') {
						if (readDatum(-1, depth) == null) {
							throw loadingException(
								"Expected datum following `#;'"
							);
						}
						continue;
					}
					unreadChar(w);
				}
			}

			if (v == '(' || v == '[' || v == '{') {
				if (depth >= maxDepth) {
					throw loadingException(
						"Lists nested deeper than " + maxDepth
					);
				}
				Deque<Datum> q = new ArrayDeque<>();
				Token o;
				sg = 0;  /* stage */
				while (( o = readDatum(v, depth + 1) ) != null) {
					if (sg == 0) {
						if (o == Token.DOT) {
							if (q.isEmpty()) {
								throw loadingException(
									"Unexpected `.'"
								);
							}
							sg++;
						} else {
							if (q.size() >= maxLength) {
								throw loadingException(
									"List longer than " + maxLength
								);
							}
							q.push(o.value);
						}
					}
					else if (sg == 1) {
						if (o == Token.DOT) {
							throw loadingException(
								"Unexpected `.'"
							);
						}
						q.push(o.value);
						sg++;
					}
					else {
						throw loadingException(
							"Excess datum following `.'"
						);
					}
				}
				if (sg == 0) {
					e = Nil.INSTANCE;
				} else if (sg == 2) {
					e = q.pop();
				} else {
					throw loadingException(
						"Missing datum following `.'"
					);
				}
				if (q.isEmpty()) {
					return new Token(e);
				} else {
					return new Token(makeList(q, q.pop(), e));
				}
			}

			if (v == ')' || v == ']' || v == '}') {
				throw loadingException(
					"Mismatched `" + v + "'"
				);
			}

			sb = new StringBuilder();
			if (v == '"') {
				readQuote(sb);
				return new Token(makeString(sb.toString()));
			}

			if (v == '|') {
				if (( v = readWord(sb, v) ) != -1) {
					unreadChar(v);
				}
				return new Token(makeSymbol(sb.toString()));
			}

			if (isPOSIXdigit(v)) {
				sb.append((char) v);
				if (( v = readPlain(sb) ) != -1) {
					unreadChar(v);
				}
				return new Token(makeSimple(sb.toString()));
			}
			else if (v == '-' || v == '+') {
				if (( w = readChar() ) != -1) {
					if (isPOSIXdigit(w)) {
						sb.append((char) v);
						sb.append((char) w);
						if (( v = readPlain(sb) ) != -1) {
							unreadChar(v);
						}
						return new Token(makeSimple(sb.toString()));
					}
					if (w == '.') {
						sb.append((char) v);
						v = w;  /* rotate */
						if (( w = readChar() ) != -1) {
							if (isPOSIXdigit(w)) {
								sb.append((char) v);
								sb.append((char) w);
								if (( v = readPlain(sb) ) != -1) {
									unreadChar(v);
								}
								return new Token(makeSimple(sb.toString()));
							}
							unreadChar(w);
						}
					}
					else {
						unreadChar(w);
					}
				}
			}

			if (( v = readWord(sb, v) ) != -1) {
				unreadChar(v);
			}
			return new Token(makeSymbol(sb.toString()));
		}

		if (bg == -1) {
			return null;
		} else {
			throw loadingException("Unterminated `" + (char) bg + "' list");
		}
	}

	private void
	skipBlockComment()
	throws
		IOException
	{
		int v, w;
		int d = 1;

		while (d > 0 && ( v = readChar() ) != -1) {
			if (v == '#') {
				if (( w = readChar() ) != -1
					&& w == '|'
				) {
					d++;
					continue;
				}
				if (w != -1) {
					unreadChar(w);
				}
			}
			else if (v == '|') {
				if (( w = readChar() ) != -1
					&& w == '#'
				) {
					d--;
					continue;
				}
				if (w != -1) {
					unreadChar(w);
				}
			}
		}
	}

	private int
	readPlain(StringBuilder sb)
	throws
		IOException
	{
		int v;

		while (( v = readChar() ) != -1) {
			if (isPOSIXspace(v)) {
				return -1;
			}

			if (v == '"' || v == '|' || v == ';'
				|| v == '(' || v == ')'
				|| v == '[' || v == ']'
				|| v == '{' || v == '}'
			) {
				break;
			}

			sb.append((char) v);
		}

		return v;
	}

	private static int
	parseDigit(int v, int radix)
	{
		int d = v >= '0' && v <= '9' ? v - '0'
			: v >= 'a' && v <= 'f' ? v - 'a' + 10
			: v >= 'A' && v <= 'F' ? v - 'A' + 10
			: -1;
		return d < radix ? d : -1;
	}

	private int
	readEscapeNumber(int radix, int limit, int v, boolean curly)
	throws
		IOException
	{
		int value = 0;
		int count = 0;
		for (;;) {
			int digit = parseDigit(v, radix);
			if (digit < 0) {
				if (curly && v != '}') {
					throw loadingException(
						"Expected base-" + radix + " digit "
						+ "or closing brace in escape");
				}
				if (!curly && v != -1) {
					unreadChar(v);
				}
				return value;
			}
			if (value > (Integer.MAX_VALUE - digit) / radix) {
				throw loadingException("Code point overflow");
			}
			value = value * radix + digit;
			if (++count == limit) {
				return value;
			}
			v = readChar();
		}
	}

	private void
	strictAppendCodePoint(StringBuilder sb, int value)
	{
		if (!Character.isValidCodePoint(value)) {
			throw loadingException("Invalid code point");
		}
		sb.appendCodePoint(value);
	}

	private void
	readQuote(StringBuilder sb)
	throws
		IOException
	{
		int v, w;
		while (( v = readChar() ) != -1) {
			if (v == '"') {
				return;
			}
			if (v != '\\') {
				sb.append((char) v);
				continue;
			}
			v = readChar();
			switch (v) {
			case -1:
				throw loadingException("Unterminated quote");
			case 'a': sb.append('\007'); break;  /* BEL */
			case 'b': sb.append('\010'); break;  /* BS (backspace) */
			case 't': sb.append('\011'); break;  /* HT (h-tab) */
			case 'n': sb.append('\012'); break;  /* LF (new line) */
			case 'v': sb.append('\013'); break;  /* VT (v-tab) */
			case 'f': sb.append('\014'); break;  /* FF (new page) */
			case 'r': sb.append('\015'); break;  /* CR */
			case 'e': sb.append('\033'); break;  /* ESC */
			case 'c':
				w = readChar();
				if (w < 0 || w > 127) {
					throw loadingException(
						"Expected ASCII character "
						+ "after control escape");
				}
				sb.append((char) (w ^ '@'));
				break;
			case 'x': case 'o':  /* queso... 😋 */
				w = readChar();
				if (v == 'o' && w != '{') {
					throw loadingException(
						"Expected opening brace "
						+ "after octal escape");
				}
				boolean curly = (w == '{');
				strictAppendCodePoint(sb, readEscapeNumber(v == 'x' ? 16 : 8,
					curly ? -1 : 2, curly ? readChar() : w, curly));
				break;
			case '0': case '1': case '2': case '3':
			case '4': case '5': case '6': case '7':
				strictAppendCodePoint(sb, readEscapeNumber(8, 3, v, false));
				break;
			case ' ': case '\t': case '\r': case '\n':
				/* XXX: I don't know what this case means? */
				while (v == ' ' || v == '\t') {
					v = readChar();
				}
				if (v != '\r' && v != '\n') {
					throw loadingException(
						"Expected line ending in escape"
					);
				}
				if (v == '\r') {
					v = readChar();
					if (v != '\n' && v != -1) {
						unreadChar(v);
					}
				}
				while (( v = readChar() ) == ' ' || v == '\t') {
					/* Discard indentation following the line ending. */
				}
				if (v != -1) {
					unreadChar(v);
				}
				break;
			default:
				sb.append((char) v);
			}
		}
		throw loadingException("Unterminated quote \"...\"");
	}

	private int
	readWord(StringBuilder sb, int v)
	throws
		IOException
	{
		boolean bar;
		int w;

		if (!( bar = (v == '|') )) {
			sb.append((char) v);
		}

		for (;;) {
			if (bar) {
				if (( v = readChar() ) == -1) {
					break;
				}
				if (v == '|') {
					bar = false;
				}
				else {
					if (v == '\\') {
						if (( w = readChar() ) != -1) {
							v = w;
						}
					}
					sb.append((char) v);
				}
			}
			else {
				if (( v = readPlain(sb) ) == '|') {
					bar = true;
				}
				else {
					break;
				}
			}
		}

		if (bar) {
			throw loadingException("Unterminated quote |...|");
		}

		return v;
	}
}
