package org.rapidcow.trivnote.scripts;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.rapidcow.trivnote.libexpr.Cons;
import org.rapidcow.trivnote.libexpr.DataLoader;
import org.rapidcow.trivnote.libexpr.Datum;

public class PreviewAppleNotesDrawing
{
	public static void main(String[] args) throws IOException
	{
		Path path;

		if (args.length != 1) {
			System.err.println("usage: PreviewAppleNotesDrawing FILE");
			System.exit(1);
		}

		path = Path.of(args[0]);
		try (BufferedReader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			DataLoader loader = new DataLoader(in);
			Datum datum = loader.readDatum();
			if (!(datum instanceof Cons)) {
				throw new AssertionError("expected a Cons, got " + datum);
			}
			System.out.println("OK: top-level datum is a Cons");
		}
	}
}
