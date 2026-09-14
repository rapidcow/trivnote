package org.rapidcow.trivnote.libexpr;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * s-expression writer.
 */
public class DataDumper
{
	protected final Writer out;

	public DataDumper(Writer file)
	{
		this.out = file;
	}

	public DataDumper(OutputStream file, Charset charset) throws IOException
	{
		this(new OutputStreamWriter(file, charset));
	}

	public DataDumper(OutputStream file) throws IOException
	{
		this(file, StandardCharsets.UTF_8);
	}
}
