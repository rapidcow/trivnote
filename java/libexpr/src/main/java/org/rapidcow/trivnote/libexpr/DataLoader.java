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
	protected final PushbackReader in;

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

	private int
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

	private void
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
	public Iterator<Datum>
	iterator()
	{
		return this;
	}

	/**
	 * {@return whether no I/O error occurred, and any datum remains}
	 * If false, caller should check {@link #eio()} to distinguish
	 * I/O error and EOF.
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
	 * Caller should check {@link #eio()} to check for
	 * I/O error and EOF.
	 *
	 * @throws NoSuchElementException on I/O error, or EOF
	 * @throws ParseException on syntax error
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
	public IOException
	eio()
	{
		return eieio;
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
			throw new DataLoadingException("Unexpected `.'", row, col);
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
						return new Token(new Scalar(sb.toString()));
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
							throw new DataLoadingException(
								"Expected datum following `#;'",
								row, col
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
								throw new DataLoadingException(
									"Unexpected `.'",
									row, col
								);
							}
							sg++;
						} else {
							q.push(tk.value);
						}
					}
					else if (sg == 1) {
						if (tk.dot) {
							throw new DataLoadingException(
								"Unexpected `.'",
								row, col
							);
						}
						q.push(tk.value);
						sg++;
					}
					else {
						throw new DataLoadingException(
							"Excess datum following `.'",
							row, col
						);
					}
				}
				if (sg == 0) {
					q.push(Nil.INSTANCE);
				} else if (sg == 1) {
					throw new DataLoadingException(
						"Missing datum following `.'",
						row, col
					);
				}
				s = q.pop();
				while (!q.isEmpty()) {
					s = new Cons(q.pop(), s);
				}
				return new Token(s);
			}

			if (v == ')' || v == ']' || v == '}') {
				throw new DataLoadingException(
					"Mismatched `" + v + "'",
					row, col
				);
			}

			sb = new StringBuilder();
			if (v == '"') {
				readQuote(sb);
				return new Token(new Scalar(sb.toString()));
			}

			if (v == '|') {
				if (( v = readWord(sb, v) ) != -1) {
					unreadChar(v);
				}
				return new Token(Symbol.of(sb.toString()));
			}

			if (isPOSIXdigit(v)) {
				sb = new StringBuilder().append((char) v);
				if (( v = readPlain(sb) ) != -1) {
					unreadChar(v);
				}
				return new Token(new Scalar(sb.toString()));
			}
			else if (v == '-' || v == '+') {
				if (( w = readChar() ) != -1) {
					if (isPOSIXdigit(w)) {
						sb.append((char) v);
						sb.append((char) w);
						if (( v = readPlain(sb) ) != -1) {
							unreadChar(v);
						}
						return new Token(new Scalar(sb.toString()));
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
								return new Token(new Scalar(sb.toString()));
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
			return new Token(Symbol.of(sb.toString()));
		}

		if (bg == -1) {
			return null;
		} else {
			throw new DataLoadingException("Unterminated `" + (char) bg + "' list", row, col);
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
	escapeDigit(int v, int radix)
	{
		int d = v >= '0' && v <= '9' ? v - '0'
			: v >= 'a' && v <= 'f' ? v - 'a' + 10
			: v >= 'A' && v <= 'F' ? v - 'A' + 10 : -1;
		return d < radix ? d : -1;
	}

	private int
	readEscapeNumber(int radix, int limit, int v, boolean braced)
	throws
		IOException
	{
		int value = 0, count = 0;
		for (;;) {
			int digit = escapeDigit(v, radix);
			if (digit < 0) {
				if (braced && v != '}') {
					throw new DataLoadingException("Expected digit or closing brace in escape", row, col);
				}
				if (!braced && v != -1) { unreadChar(v); }
				return value;
			}
			if (value > (Integer.MAX_VALUE - digit) / radix) {
				throw new DataLoadingException("Escape exceeds MAX_INT", row, col);
			}
			value = value * radix + digit;
			if (++count == limit) { return value; }
			v = readChar();
		}
	}

	private void
	appendEscapeCodePoint(StringBuilder sb, int value)
	{
		if (!Character.isValidCodePoint(value)) {
			throw new DataLoadingException("Invalid escape code point", row, col);
		}
		sb.appendCodePoint(value);
	}

	private void
	readQuote(StringBuilder sb)
	throws
		IOException
	{
		int v, w;
		while ((v = readChar()) != -1) {
			if (v == '"') { return; }
			if (v != '\\') { sb.append((char) v); continue; }
			v = readChar();
			switch (v) {
			case -1:
				throw new DataLoadingException("Unterminated quote", row, col);
			case 'a': sb.append('\007'); break;
			case 'b': sb.append('\b'); break;
			case 't': sb.append('\t'); break;
			case 'n': sb.append('\n'); break;
			case 'r': sb.append('\r'); break;
			case 'e': sb.append('\033'); break;
			case 'c':
				w = readChar();
				if (w < 0 || w > 127) {
					throw new DataLoadingException("Expected ASCII character after control escape", row, col);
				}
				sb.appendCodePoint(w ^ '@');
				break;
			case 'x': case 'o':
				w = readChar();
				if (v == 'o' && w != '{') {
					throw new DataLoadingException("Expected opening brace after octal escape", row, col);
				}
				boolean braced = w == '{';
				appendEscapeCodePoint(sb, readEscapeNumber(v == 'x' ? 16 : 8,
					braced ? -1 : 2, braced ? readChar() : w, braced));
				break;
			case '0': case '1': case '2': case '3':
			case '4': case '5': case '6': case '7':
				appendEscapeCodePoint(sb, readEscapeNumber(8, 3, v, false));
				break;
			case ' ': case '\t': case '\r': case '\n':
				while (v == ' ' || v == '\t') { v = readChar(); }
				if (v != '\r' && v != '\n') {
					throw new DataLoadingException("Expected line ending in escape", row, col);
				}
				if (v == '\r') {
					v = readChar();
					if (v != '\n' && v != -1) { unreadChar(v); }
				}
				while ((v = readChar()) == ' ' || v == '\t') {
					/* Discard indentation following the line ending. */
				}
				if (v != -1) { unreadChar(v); }
				break;
			default: sb.appendCodePoint(v);
			}
		}
		throw new DataLoadingException("Unterminated quote", row, col);
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
			throw new DataLoadingException("Unterminated quote |...|", row, col);
		}

		return v;
	}
}
