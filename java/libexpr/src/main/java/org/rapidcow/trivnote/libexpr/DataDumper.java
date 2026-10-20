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
		// TODO
		out.write(name.value());
	}

	private void
	writeScalar(Scalar leaf)
	throws
		IOException
	{
		// TODO
		if (!leaf.isString()) {
			out.write(leaf.value());
			return;
		}
		out.write("\"");
		out.write(leaf.value().replace("\"", "\\\""));
		out.write("\"");
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
			writeDatum(node);
			if (once) {
				once = false;
			} else {
				out.write(" ");
			}
		}
		node = walk.rest();
		if (node != Nil.INSTANCE) {
			out.write(" . ");
			writeDatum(node);
		}
		out.write(")");
	}
}
