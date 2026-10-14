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
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * R7RS-ish S-expression reader.
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
	 * Reads one character from the underlying reader and
	 * advances the cursor position used by loadingException.
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
	 * Pushes back a character and restores the cursor position
	 * to where it was before that character was read.
	 * Only the most recent call to readChar can be undone this
	 * way, and only once; the position is not restored correctly
	 * if this is called twice in a row.
	 *
	 * @param v  the character most recently returned by readChar
	 * @throws IOException on I/O error
	 */
	protected final void
	unreadChar(int v)
	throws
		IOException
	{
		if (v == '\n') {
			row--;
		}
		col = ocl;
		in.unread(v);
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
	 * @return the interned symbol for value by default
	 */
	protected Symbol
	makeSymbol(String value)
	{
		return Symbol.of(value);
	}

	/**
	 * Hook called when a quoted string token has been read.
	 * The cursor is already past the closing quote, so a position
	 * reported from here points just after the token.
	 *
	 * @param value  the text of the string, with escapes processed
	 * @return a string scalar by default
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
	 */
	protected Scalar
	makeNumber(String value)
	{
		return Scalar.of(value);
	}

	/**
	 * {@return a new exception carrying the current cursor position}
	 * The exception is returned, not thrown, so the caller writes
	 * "throw loadingException(...)" and the compiler sees that
	 * control does not continue past it.
	 *
	 * @param message  the detail message
	 */
	protected final DataLoadingException
	loadingException(String message)
	{
		return new DataLoadingException(message, row, col);
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
		Token tk = readDatum(-1);
		if (tk == null) {
			return null;
		}
		if (tk.dot) {
			throw loadingException("Unexpected `.'");
		}
		return tk.value;
	}

	private Token
	readDatum(int bg)
	throws
		IOException
	{
		int v, w, eg, sg;
		Datum s;
		Token tk;
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
						return new Token(makeNumber(sb.toString()));
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
						if (readDatum(-1) == null) {
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
				Deque<Datum> q = new ArrayDeque<>();
				sg = 0;  /* stage */
				while (( tk = readDatum(v) ) != null) {
					if (sg == 0) {
						if (tk.dot) {
							if (q.isEmpty()) {
								throw loadingException(
									"Unexpected `.'"
								);
							}
							sg++;
						} else {
							q.push(tk.value);
						}
					}
					else if (sg == 1) {
						if (tk.dot) {
							throw loadingException(
								"Unexpected `.'"
							);
						}
						q.push(tk.value);
						sg++;
					}
					else {
						throw loadingException(
							"Excess datum following `.'"
						);
					}
				}
				if (sg == 0) {
					q.push(Nil.INSTANCE);
				} else if (sg == 1) {
					throw loadingException(
						"Missing datum following `.'"
					);
				}
				s = q.pop();
				while (!q.isEmpty()) {
					s = new Cons(q.pop(), s);
				}
				return new Token(s);
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
				sb = new StringBuilder().append((char) v);
				if (( v = readPlain(sb) ) != -1) {
					unreadChar(v);
				}
				return new Token(makeNumber(sb.toString()));
			}
			else if (v == '-' || v == '+') {
				if (( w = readChar() ) != -1) {
					if (isPOSIXdigit(w)) {
						sb.append((char) v);
						sb.append((char) w);
						if (( v = readPlain(sb) ) != -1) {
							unreadChar(v);
						}
						return new Token(makeNumber(sb.toString()));
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
								return new Token(makeNumber(sb.toString()));
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
