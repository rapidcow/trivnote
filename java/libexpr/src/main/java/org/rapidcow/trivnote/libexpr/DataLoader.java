package org.rapidcow.trivnote.libexpr;

import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * R7RS-ish s-expression reader.
 */
public class DataLoader implements Iterable<Datum>, Iterator<Datum>
{
	protected final PushbackReader in;

	private Datum st = null;

	public DataLoader(Reader file)
	{
		this.in = new PushbackReader(file);
	}

	public DataLoader(InputStream file, Charset charset) throws IOException
	{
		this(new InputStreamReader(file, charset));
	}

	public DataLoader(InputStream file) throws IOException
	{
		this(file, StandardCharsets.UTF_8);
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

	@Override public Iterator<Datum> iterator()
	{
		return this;
	}

	@Override public boolean hasNext()
	{
		if (st == null) {
			try {
				st = readDatum(-1);
			} catch (IOException e) {
				/* XXX: nasty; maybe we should learn from java.util.Scanner */
				throw new java.io.UncheckedIOException(e);
			}
		}
		return st != null;
	}

	@Override public Datum next()
	{
		Datum s;
		if (hasNext()) {
			s = st;
			st = null;
			return s;
		}
		throw new NoSuchElementException();
	}

	private Datum readDatum(int bg) throws IOException
	{
		int v, w, eg;
		Cons ts;
		Datum s;
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

		while (( v = in.read() ) != -1) {
			if (isPOSIXspace(v)) {
				continue;
			}

			if (v == eg) {
				return null;
			}

			if (v == ';') {
				while (( v = in.read() ) != -1) {
					if (v == '\n') {
						break;
					}
				}
				continue;
			}

			if (v == '#') {
				if (( w = in.read() ) != -1
					&& w == '|'
				) {
					skipBlockComment();
					continue;
				}
				if (w != -1) {
					in.unread(w);
				}
			}

			if (v == '(' || v == '[' || v == '{') {
				Deque<Datum> q = new ArrayDeque<>();
				while (( s = readDatum(v) ) != null) {
					q.push(s);
				}
				if (q.isEmpty()) {
					return Nil.INSTANCE;
				} else {
					ts = new Cons(q.pop(), Nil.INSTANCE);
					while (!q.isEmpty()) {
						ts = new Cons(q.pop(), ts);
					}
					return ts;
				}
			}

			if (v == ')' || v == ']' || v == '}') {
				throw new RuntimeException(
					"Mismatched `" + v + "'"
				);
			}

			if (v == '"') {
				sb = new StringBuilder();
				readQuote(sb);
				return new Scalar(sb.toString());
			}

			if (v == '|') {
				sb = new StringBuilder();
				if (( v = readToken(sb, v) ) != -1) {
					in.unread(v);
				}
				return Symbol.of(sb.toString());
			}

			if (isPOSIXdigit(v)) {
				sb = new StringBuilder().append((char) v);
				if (( v = readPlain(sb) ) != -1) {
					in.unread(v);
				}
				return new Scalar(sb.toString());
			}
			else if (v == '-' || v == '+') {
				if (( w = in.read() ) != -1
					&& isPOSIXdigit(w)
				) {
					in.unread(w);
					sb = new StringBuilder().append((char) v);
					if (( v = readPlain(sb) ) != -1) {
						in.unread(v);
					}
					return new Scalar(sb.toString());
				}
				if (w != -1) {
					in.unread(w);
				}
			}

			sb = new StringBuilder();
			if (( v = readToken(sb, v) ) != -1) {
				in.unread(v);
			}
			return Symbol.of(sb.toString());
		}

		return null;
	}

	private void skipBlockComment() throws IOException
	{
		int v, w;
		int d = 1;

		while (d > 0 && ( v = in.read() ) != -1) {
			if (v == '#') {
				if (( w = in.read() ) != -1
					&& w == '|'
				) {
					d++;
					continue;
				}
				if (w != -1) {
					in.unread(w);
				}
			}
			else if (v == '|') {
				if (( w = in.read() ) != -1
					&& w == '#'
				) {
					d--;
					continue;
				}
				if (w != -1) {
					in.unread(w);
				}
			}
		}
	}

	private int readPlain(StringBuilder sb) throws IOException
	{
		int v;

		while (( v = in.read() ) != -1) {
			if (isPOSIXspace(v)) {
				return -1;
			}

			if (v == '"' || v == '|'
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

		while (( v = in.read() ) != -1) {
			if (v == '"') {
				return;
			}

			if (v == '\\') {
				if (( w = in.read() ) != -1) {
					v = w;
				}
			}

			sb.append((char) v);
		}

		throw new RuntimeException("Unterminated quote");
	}

	private int readToken(StringBuilder sb, int v) throws IOException
	{
		boolean bar = (v == '|');
		int w;

		do {
			if (bar) {
				if (v == '|') {
					bar = false;
				}

				else if (v == '\\') {
					if (( w = in.read() ) != -1) {
						v = w;
					}
				}
				sb.append((char) v);
			}
			else {
				sb.append((char) v);
				if (( v = readPlain(sb) ) == '|') {
					bar = true;
				} else {
					break;
				}
			}
		} while (( v = in.read() ) != -1);

		return v;
	}
}
