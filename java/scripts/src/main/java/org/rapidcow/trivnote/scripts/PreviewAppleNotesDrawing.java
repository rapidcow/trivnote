package org.rapidcow.trivnote.scripts;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.rapidcow.trivnote.libexpr.Atom;
import org.rapidcow.trivnote.libexpr.Cons;
import org.rapidcow.trivnote.libexpr.DataLoader;
import org.rapidcow.trivnote.libexpr.Datum;
import org.rapidcow.trivnote.libexpr.Nil;
import org.rapidcow.trivnote.libexpr.Symbol;
import org.rapidcow.trivnote.libexpr.Walk;

public class PreviewAppleNotesDrawing
{
	/*
	 * equivalent to perl "croak"; go up a frame, and die.
	 *
	 * The only difference, since Java's static analysis
	 * isn't smart enough to tell that a function that
	 * always throws never returns.  So i'm just gonna call
	 * it up( ... ) so i'd remember to throw up( ... ). >n>
	 */
	private static RuntimeException up(int depth, String mess, Throwable cause)
	{
		RuntimeException up = new RuntimeException(mess, cause);
		StackTraceElement[] stack = up.getStackTrace();
		up.setStackTrace(Arrays.copyOfRange(stack, 1 + depth, stack.length));
		return up;
	}

	private static RuntimeException up(String mess)
	{
		return up(1, mess, null);
	}

	private static RuntimeException up(String mess, Throwable cause)
	{
		return up(1, mess, cause);
	}

	public static void main(String[] args) throws IOException
	{
		Path path;
		Datum root;

		if (args.length != 1) { throw up( "usage: PreviewAppleNotesDrawing FILE" ); }

		path = Path.of(args[0]);
		try (BufferedReader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			DataLoader loader = new DataLoader(in);
			root = loader.readDatum();
			if (loader.hasNext()) { throw up( "trailing garbage" ); }
			if (loader.eio() != null) { throw up( "I/O error", loader.eio() ); }
		}

		Datum rK, rV;
		Map<String,Datum> drawing;

		if (root instanceof Cons r) {
			rK = r.car();
			rV = r.cdr();
		} else {
			throw up( "root node not a cons cell: " + root );
		}

		if (!( Symbol.of("drawing").equals(rK) )) {
			throw up( "bad root key: " + root );
		}

		drawing = toAlist(rV);
		System.out.println("{");
		for (Map.Entry<String,Datum> entry : drawing.entrySet()) {
			System.out.println("  " + entry.getKey() + " => " + entry.getValue() + ",");
		}
		System.out.println("}");
	}

	private static Map<String,Datum> toAlist(Datum node)
	{
		Walk it;
		Map<String,Datum> jc;

		if (!( node.isList() )) { throw up( "not a list" ); }
		if (node == Nil.INSTANCE) {
			return Collections.emptyMap();
		}

		it = new Walk(node);
		jc = new HashMap<>();

		while (it.hasNext()) {
			Datum elt = it.next();
			Datum car;
			Datum cdr;
			Atom word;

			if ( elt instanceof Cons cp ) {
				car = cp.car();
				cdr = cp.cdr();
			}
			else { throw up( "alist entry not a cons cell: " + elt ); }

			if ( car instanceof Atom str ) {
				word = str;
			}
			else { throw up( "alist car not an atom" ); }

			if (( jc.putIfAbsent(word.value(), cdr) ) != null) {
				throw up( "alist duplicate key: " + word );
			}
		}

		if (it.tail() != Nil.INSTANCE) {
			throw up( "improper list (tail is: " + it.tail() + ")" );
		}
		return jc;
	}
}
