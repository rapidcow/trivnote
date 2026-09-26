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
public class DataLoader implements Iterable<Datum>, Iterator<Datum>
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
	public DataLoader(Reader file)
	{
		this.in = new PushbackReader(file);
	}

	/**
	 * Creates a reader from a byte stream.
	 *
	 * @param file    input byte stream
	 * @param charset character encoding
	 */
	public DataLoader(InputStream file, Charset charset)
	{
		this(new InputStreamReader(file, charset));
	}

	/**
	 * Creates a reader from a byte stream with the default
	 * character encoding, {@link StandardCharsets#UTF_8}.
	 *
	 * @param file  input byte stream
	 */
	public DataLoader(InputStream file)
	{
		this(file, StandardCharsets.UTF_8);
	}

	/**
	 * Parsing exception.
	 */
	public class ParseException extends RuntimeException
	{
		/* Exceptions are serializable, so Javadoc is necessary. */

		/**
		 * Line offset (0-indexed).
		 */
		private final long lineOffset;

		/**
		 * Column offset (0-indexed).
		 */
		private final long charOffset;

		/**
		 * Creates a new parsing exception without chaining.
		 *
		 * @param message    passed to {@link RuntimeException#RuntimeException(String)}
		 * @param lineOffset line offset (0-indexed)
		 * @param charOffset column offset (0-indexed)
		 */
		public ParseException(
			String message,
			long lineOffset,
			long charOffset
		)
		{
			super(message);
			this.lineOffset = lineOffset;
			this.charOffset = charOffset;
		}

		/**
		 * Creates a new parsing exception with chaining.
		 *
		 * @param message   passed to {@link RuntimeException#RuntimeException(String, Throwable)}
		 * @param cause     passed to {@link RuntimeException#RuntimeException(String, Throwable)}
		 * @param lineOffset line offset (0-indexed)
		 * @param charOffset column offset (0-indexed)
		 */
		public ParseException(
			String message,
			Throwable cause,
			long lineOffset,
			long charOffset
		)
		{
			super(message, cause);
			this.lineOffset = lineOffset;
			this.charOffset = charOffset;
		}

		@Override public String toString()
		{
			return ("line " + getLineNumber() +
				" char " + getCharNumber() +
				": " + super.toString());
		}

		/**
		 * {@return the line offset (0-based) of this exception}
		 */
		public long getLineOffset()
		{
			return row;
		}

		/**
		 * {@return the line number (1-based) of this exception}
		 */
		public long getLineNumber()
		{
			return row + 1;
		}

		/**
		 * {@return the column number (1-based) of this exception}
		 */
		public long getCharOffset()
		{
			return col;
		}

		/**
		 * {@return the column number (1-based) of this exception}
		 */
		public long getCharNumber()
		{
			return col + 1;
		}
	}

	private static class Token
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

	private static boolean isPOSIXspace(int v)
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

	private static boolean isPOSIXdigit(int v)
	{
		return '0' <= v && v <= '9';
	}

	private int readChar() throws IOException
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

	private void unreadChar(int v) throws IOException
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
	@Override public Iterator<Datum> iterator()
	{
		return this;
	}

	/**
	 * {@return whether no I/O error occurred, and any datum remains}
	 * If false, caller should check {@link #eio()} to distinguish
	 * I/O error and EOF.
	 */
	@Override public boolean hasNext()
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
	@Override public Datum next()
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
	public IOException eio()
	{
		return eieio;
	}

	/**
	 * Reads and returns the next datum.
	 *
	 * @return the next datum, or {@code null} on EOF
	 * @throws IOException on {@link Reader#read} error
	 */
	public Datum readDatum() throws IOException
	{
		Token tk = readDatum(-1);
		if (tk == null) {
			return null;
		}
		if (tk.dot) {
			throw new ParseException("Unexpected `.'", row, col);
		}
		return tk.value;
	}

	private Token readDatum(int bg) throws IOException
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
				if (( w = readChar() ) != -1) {
					if (isPOSIXspace(w)) {
						return Token.DOT;
					}
					else if (
						w == '(' || w == '[' || w == '{' ||
						w == ')' || w == ']' || w == '}'
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
							throw new ParseException(
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
								throw new ParseException(
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
							throw new ParseException(
								"Unexpected `.'",
								row, col
							);
						}
						q.push(tk.value);
						sg++;
					}
					else {
						throw new ParseException(
							"Excess datum following `.'",
							row, col
						);
					}
				}
				if (sg == 0) {
					q.push(Nil.INSTANCE);
				} else if (sg == 1) {
					throw new ParseException(
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
				throw new ParseException(
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
			throw new ParseException("Unterminated `" + (char) bg + "' list", row, col);
		}
	}

	private void skipBlockComment() throws IOException
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

	private int readPlain(StringBuilder sb) throws IOException
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

	private void readQuote(StringBuilder sb) throws IOException
	{
		int v, w;

		while (( v = readChar() ) != -1) {
			if (v == '"') {
				return;
			}

			if (v == '\\') {
				if (( w = readChar() ) != -1) {
					v = w;
				}
			}

			sb.append((char) v);
		}

		throw new ParseException("Unterminated quote \"...\"", row, col);
	}

	private int readWord(StringBuilder sb, int v) throws IOException
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
			throw new ParseException("Unterminated quote |...|", row, col);
		}

		return v;
	}
}
