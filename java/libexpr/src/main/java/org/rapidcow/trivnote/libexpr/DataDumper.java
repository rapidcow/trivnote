/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static org.rapidcow.trivnote.libexpr.DataLoader.isPOSIXdigit;

/**
 * S-expression writer.
 */
public
class
	DataDumper
{
	/**
	 * Output character stream.
	 */
	protected final Writer out;

	private long maxDepth = Long.MAX_VALUE;
	private long maxLength = Long.MAX_VALUE;
	private boolean unicode;

	/**
	 * Limits list nesting, using the same convention as
	 * {@link DataLoader#useDepthLimit(long)}.  Atoms have depth zero;
	 * even an empty list needs depth one.  The default is unlimited.
	 * The writer recurses per level; a high limit can still exhaust
	 * the call stack.
	 * @param depth maximum list nesting
	 * @return this
	 * @throws IllegalArgumentException if depth is negative
	 */
	public final DataDumper
	useDepthLimit(long depth)
	{
		if (depth < 0) {
			throw new IllegalArgumentException("Negative depth");
		}
		maxDepth = depth;
		return this;
	}

	/**
	 * Removes the depth limit.  Limits otherwise persist across calls.
	 * @return this
	 */
	public final DataDumper
	noMaxDepth()
	{
		return useDepthLimit(Long.MAX_VALUE);
	}

	/**
	 * Limits elements per list, excluding a dotted tail, as in
	 * {@link DataLoader#useLengthLimit(long)}.  The default is unlimited.
	 * This does not limit string length or total output size.
	 * @param length maximum elements per list
	 * @return this
	 * @throws IllegalArgumentException if length is negative
	 */
	public final DataDumper
	useLengthLimit(long length)
	{
		if (length < 0) {
			throw new IllegalArgumentException("Negative length");
		}
		maxLength = length;
		return this;
	}

	/**
	 * Removes the length limit.  Limits otherwise persist across calls.
	 * @return this
	 */
	public final DataDumper
	noMaxLength()
	{
		return useLengthLimit(Long.MAX_VALUE);
	}

	/**
	 * Allows literal Unicode in strings and quoted symbols.  By default
	 * output is ASCII.  Controls and unpaired surrogates stay escaped.
	 * The output encoding must support the characters being written.
	 * @param enabled whether to emit Unicode literally
	 * @return this
	 */
	public final DataDumper
	useUnicode(boolean enabled)
	{
		unicode = enabled;
		return this;
	}

	/**
	 * Creates a writer from a character stream.
	 *
	 * @param file  output character stream
	 */
	public
	DataDumper(Writer file)
	{
		this.out = file;
	}

	/**
	 * Creates a writer from a byte stream.
	 *
	 * @param file    output byte stream
	 * @param charset character encoding
	 */
	public
	DataDumper(OutputStream file, Charset charset)
	{
		this(new OutputStreamWriter(file, charset));
	}

	/**
	 * Creates a writer from a byte stream with the default
	 * character encoding, {@link StandardCharsets#UTF_8}.
	 *
	 * @param file  output byte stream
	 */
	public
	DataDumper(OutputStream file)
	{
		this(file, StandardCharsets.UTF_8);
	}

	/**
	 * Constructs a new dumping exception.
	 *
	 * @param message  exception message
	 * @return a new dumping exception.
	 */
	protected DataDumpingException
	dumpingException(String message)
	{
		return new DataDumpingException(message);
	}

	/**
	 * Writes one datum.  Failure may leave partial output in the stream.
	 * @param node datum to write
	 * @throws IOException on output failure
	 * @throws DataDumpingException if a configured limit is exceeded
	 */
	public void
	writeDatum(Datum node)
	throws
		IOException
	{
		writeDatum(node, 0);
	}

	private void
	writeDatum(Datum node, long depth)
	throws
		IOException
	{
		if (Datum.isList(node) && depth >= maxDepth) {
			throw dumpingException("Lists nested deeper than " + maxDepth);
		}
		if (node == Nil.INSTANCE) {
			out.write("()");
			return;
		}
		if (node instanceof Symbol name) {
			writeSymbol(name.value());
			return;
		}
		if (node instanceof BoolScalar flag) {
			writeBool(flag.booleanValue());
			return;
		}
		if (node instanceof NumberScalar number) {
			writeNumber(number);
			return;
		}
		if (node instanceof StringScalar text) {
			writeString(text.value());
			return;
		}
		if (node instanceof OctetVector octets) {
			writeOctets(octets);
			return;
		}
		if (node instanceof Cons cell) {
			DatumWalk walk = new DatumWalk(cell);
			Datum item;
			out.write("(");
			boolean once = true;
			while (walk.hasNext()) {
				if (walk.position() >= maxLength) {
					throw dumpingException("List longer than " + maxLength);
				}
				item = walk.next();
				if (once) {
					once = false;
				} else {
					out.write(" ");
				}
				writeDatum(item, 1 + depth);
			}
			item = walk.rest();
			if (item != Nil.INSTANCE) {
				out.write(" . ");
				writeDatum(item, 1 + depth);
			}
			out.write(")");
			return;
		}
		/* This is impossible to reach as Datum is sealed. */
		throw new IllegalStateException();
	}

	/**
	 * Writes a symbol, quoted with {@code |...|} if the reader
	 * would not otherwise read it back as the same symbol.
	 * @param value name of the symbol
	 * @throws IOException on output failure
	 */
	public void
	writeSymbol(String value)
	throws
		IOException
	{
		if (!needsQuoting(value)) {
			out.write(value);
			return;
		}
		out.write("|");
		writeEscaped(value, '|');
		out.write("|");
	}

	/**
	 * {@return whether the reader would misread {@code value}
	 * as something other than a symbol if written bare}
	 */
	private static boolean
	needsQuoting(String value)
	{
		if (value.isEmpty() || value.equals(".") || value.charAt(0) == '#') {
			return true;
		}
		if (value.equals("+nan.0") || value.equals("-nan.0")
			|| value.equals("+inf.0") || value.equals("-inf.0"))
		{
			return true;
		}
		for (int i = 0; i < value.length(); i++) {
			char v = value.charAt(i);
			if (v <= ' ' || v >= '\177') {
				/* whitespace, control, DEL or non-ASCII */
				return true;
			}
			switch (v) {
			case '(': case ')':
			case '[': case ']':
			case '{': case '}':
			case '"': case '|': case ';':
				return true;
			}
		}
		return looksNumeric(value);
	}

	/**
	 * {@return whether the reader would start parsing {@code value}
	 * as a number}  Only the beginning matters: {@code 1cm} and
	 * {@code -.4cm} count, {@code -.cm} does not.
	 */
	private static boolean
	looksNumeric(String value)
	{
		int i = 0;
		if (value.charAt(i) == '+' || value.charAt(i) == '-') {
			i++;
		}
		if (i < value.length() && value.charAt(i) == '.') {
			i++;
		}
		return i < value.length() && isPOSIXdigit(value.charAt(i));
	}

	/**
	 * Writes a number as {@link NumberScalar#value()}.
	 * @param number number to write
	 * @throws IOException on output failure
	 */
	public void
	writeNumber(NumberScalar number)
	throws
		IOException
	{
		out.write(number.value());
	}

	/**
	 * Writes an octet vector, as {@code #u8(...)} with decimal octets.
	 * The length limit applies; the depth limit does not, as a vector
	 * holds no nested data.
	 * @param octets octet vector to write
	 * @throws IOException on output failure
	 * @throws DataDumpingException if a configured limit is exceeded
	 */
	public void
	writeOctets(OctetVector octets)
	throws
		IOException
	{
		int n = octets.length();
		out.write("#u8(");
		for (int i = 0; i < n; i++) {
			if (i > 0) {
				out.write(" ");
			}
			out.write(Integer.toString(octets.get(i)));
		}
		out.write(")");
	}

	/**
	 * Writes a boolean, as {@code #t} or {@code #f}.
	 * @param value boolean to write
	 * @throws IOException on output failure
	 */
	public void
	writeBool(boolean value)
	throws
		IOException
	{
		out.write(value ? "#t" : "#f");
	}

	/**
	 * Writes a quoted string.
	 * @param value contents of the string
	 * @throws IOException on output failure
	 */
	public void
	writeString(String value)
	throws
		IOException
	{
		out.write("\"");
		writeEscaped(value, '"');
		out.write("\"");
	}

	/**
	 * Writes the body of a {@code "..."} string or a {@code |...|}
	 * symbol.  Delimiters and controls are escaped, as is non-ASCII
	 * text unless Unicode output is enabled.
	 *
	 * @param value  the text to write
	 * @param quote  the delimiter, {@code '"'} or {@code '|'}
	 */
	private void
	writeEscaped(String value, int quote)
	throws
		IOException
	{
		int i, v, w;
		for (i = 0; i < value.length(); ) {
			v = value.codePointAt(i);
			i += Character.charCount(v);
			switch (v) {
			case '\\': out.write("\\\\"); break;
			case '\007': out.write("\\a"); break;
			case '\010': out.write("\\b"); break;
			case '\011': out.write("\\t"); break;
			case '\012': out.write("\\n"); break;
			case '\013': out.write("\\v"); break;
			case '\014': out.write("\\f"); break;
			case '\015': out.write("\\r"); break;
			case '\033': out.write("\\e"); break;
			default:
				if (v == quote) {
					out.write('\\');
					out.write(v);
				} else if (' ' <= v && v < '\177') {
					out.write(v);
				} else if (unicode && !Character.isISOControl(v)
					&& !(v >= 0xd800 && v <= 0xdfff))
				{
					out.write(Character.toChars(v));
				} else {
					if (v > '\377') {
						out.write("\\x{");
						out.write(Integer.toHexString(v));
						out.write('}');
					}
					else if (v >= '\100' || (i < value.length() &&
						'0' <= (w = value.codePointAt(i)) && w <= '7'))
					{
						out.write('\\');
						out.write((char) ('0' + (v >> 6)));
						out.write((char) ('0' + ((v >> 3) & 7)));
						out.write((char) ('0' + (v & 7)));
					}
					else {
						out.write('\\');
						if (v >= 8) {
							out.write((char) ('0' + ((v >> 3) & 7)));
						}
						out.write((char) ('0' + (v & 7)));
					}
				}
			}
		}
	}
}
