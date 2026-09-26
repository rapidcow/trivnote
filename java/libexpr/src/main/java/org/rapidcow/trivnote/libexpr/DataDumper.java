package org.rapidcow.trivnote.libexpr;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * S-expression writer.  This class is incomplete.
 */
public class DataDumper
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
	public DataDumper(Writer file)
	{
		this.out = file;
	}

	/**
	 * Creates a writer from a byte stream.
	 *
	 * @param file    output byte stream
	 * @param charset character encoding
	 */
	public DataDumper(OutputStream file, Charset charset)
	{
		this(new OutputStreamWriter(file, charset));
	}

	/**
	 * Creates a writer from a byte stream with the default
	 * character encoding, {@link StandardCharsets#UTF_8}.
	 *
	 * @param file  output byte stream
	 */
	public DataDumper(OutputStream file)
	{
		this(file, StandardCharsets.UTF_8);
	}
}
