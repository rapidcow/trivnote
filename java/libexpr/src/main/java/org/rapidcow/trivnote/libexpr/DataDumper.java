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
 * S-expression writer.  This class is incomplete.
 */
public
class
	DataDumper
{
	/**
	 * Output character stream.
	 */
	protected final Writer out;

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

	private RuntimeException
	dumpingException(String message)
	{
		// TODO
		return new RuntimeException(message);
	}

	public void
	writeDatum(Datum node)
	throws
		IOException
	{
		if (node == Nil.INSTANCE) {
			out.write("()");
			return;
		}
		if (node instanceof Symbol name) {
			writeSymbol(name);
			return;
		}
		if (node instanceof Scalar leaf) {
			writeScalar(leaf);
			return;
		}
		if (node instanceof Cons cell) {
			writeList(cell);
			return;
		}
		/* This is impossible to reach as Datum is sealed. */
		throw new IllegalStateException();
	}

	private void
	writeSymbol(Symbol name)
	throws
		IOException
	{
		String value = name.value();
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

	private void
	writeScalar(Scalar leaf)
	throws
		IOException
	{
		if (!leaf.isString()) {
			out.write(leaf.value());
			return;
		}
		writeString(leaf.value());
	}

	/**
	 * Writes a quoted string.
	 */
	private void
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
	 * symbol, as pure printable ASCII: {@code quote}, the backslash
	 * and everything that is not printable ASCII are escaped.
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
					out.write("\\");
					out.write(v);
				} else if (' ' <= v && v < '\177') {
					out.write(v);
				} else {
					if (v > '\377') {
						out.write("\\x{");
						out.write(Integer.toHexString(v));
						out.write("}");
					}
					else if (v >= '\100' || (i < value.length() &&
						'0' <= (w = value.codePointAt(i)) && w <= '7'))
					{
						out.write("\\");
						out.write((char) ('0' + (v >> 6)));
						out.write((char) ('0' + ((v >> 3) & 7)));
						out.write((char) ('0' + (v & 7)));
					}
					else {
						out.write("\\");
						if (v >= 8) {
							out.write((char) ('0' + ((v >> 3) & 7)));
						}
						out.write((char) ('0' + (v & 7)));
					}
				}
			}
		}
	}

	private void
	writeList(Cons cell)
	throws
		IOException
	{
		// TODO (indentation?)
		DatumWalk walk = new DatumWalk(cell);
		Datum node;
		out.write("(");
		boolean once = true;
		while (walk.hasNext()) {
			node = walk.next();
			if (once) {
				once = false;
			} else {
				out.write(" ");
			}
			writeDatum(node);
		}
		node = walk.rest();
		if (node != Nil.INSTANCE) {
			out.write(" . ");
			writeDatum(node);
		}
		out.write(")");
	}
}
