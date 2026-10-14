package org.rapidcow.trivnote.scripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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
			if (loader.lastIOException() != null) { throw up( "I/O error", loader.fileException() ); }
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

		List<Ink> brushKit = loadInk(drawing.get("inks"));
		List<Stroke> strokes = loadStrokes(brushKit, drawing.get("strokes"));
		System.out.println(strokes.size()
			+ " stroke"
			+ (strokes.size() == 1 ? "" : "s")
			+ " loaded");

		SwingUtilities.invokeLater(() -> {
			DrawingPanel panel = new DrawingPanel(strokes);
			panel.setPreferredSize(new Dimension(900, 700));

			JFrame window = new JFrame("Apple Notes Drawing");
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			window.setContentPane(panel);
			window.pack();
			window.setLocationRelativeTo(null);  // Center on screen.
			window.setVisible(true);
			panel.animate(50);
		});
	}

	/*
	 * `strokesNode' is the cdr of a top-level (strokes ...) entry:
	 * a plain list of (stroke ...) tagged forms, so toAlist can't
	 * be used directly here (every entry shares the same "stroke"
	 * tag, which toAlist would reject as a duplicate key).
	 */
	private static List<Stroke> loadStrokes(List<Ink> kit, Datum strokesNode)
	{
		List<Stroke> strokes = new ArrayList<>();
		for (Datum node : tagged("stroke", strokesNode)) {
			Map<String,Datum> fields = toAlist(node);
			Clock sortId = loadClock(fields.get("sort-id"));
			String uuid = ((Atom) first(fields.get("uuid"))).value();
			Ink ink = kit.get(Integer.parseInt(((Atom) first(fields.get("ink-index"))).value()));
			AffineTransform map = loadTransformOrIdentity(fields.get("transform"), fields.get("substrokes"));
			AffineTransform inkMap = loadTransformOrIdentity(fields.get("ink-transform"), null);
			strokes.add(new Stroke(sortId, uuid, ink, map, inkMap, loadPoints(fields.get("points"))));
		}
		strokes.sort(Comparator.comparing(Stroke::clock));
		return strokes;
	}

	/*
	 * `pointsNode' is the cdr of a (points ...) entry: a plain list
	 * whose elements are themselves alists (each already shaped as
	 * ((point X Y) (altitude . R) (force . R) (radius . R) ...)),
	 * so toAlist applies directly to each element.
	 */
	private static List<Point> loadPoints(Datum pointsNode)
	{
		List<Point> points = new ArrayList<>();
		Walk it = new Walk(pointsNode);
		while (it.hasNext()) {
			Map<String,Datum> fields = toAlist(it.next());
			Cons xy = (Cons) fields.get("point");
			double x = num(xy.car());
			double y = num(((Cons) xy.cdr()).car());
			/* radius is missing on some points (e.g. dry taps) */
			double radius = fields.containsKey("radius") ? num(fields.get("radius")) : 0.0;
			points.add(new Point(x, y, radius));
		}
		if (it.tail() != Nil.INSTANCE) {
			throw up( "improper list (tail is: " + it.tail() + ")" );
		}
		return points;
	}

	/**
	 * Walks a plain list whose every element is a tagged form
	 * {@code (tag . rest)}, checking the tag along the way, and
	 * returns the {@code rest} of each element in order.
	 */
	private static List<Datum> tagged(String tag, Datum node)
	{
		List<Datum> rest = new ArrayList<>();
		Symbol wantTag = Symbol.of(tag);
		Walk it = new Walk(node);
		while (it.hasNext()) {
			Datum elt = it.next();
			if (!(elt instanceof Cons cp) || !wantTag.equals(cp.car())) {
				throw up( "expected a (" + tag + " ...) form: " + elt );
			}
			rest.add(cp.cdr());
		}
		if (it.tail() != Nil.INSTANCE) {
			throw up( "improper list (tail is: " + it.tail() + ")" );
		}
		return rest;
	}

	/** {@return the sole element of a one-element list, e.g. {@code (uuid "x")}} */
	private static Datum first(Datum d)
	{
		if (d instanceof Cons c) { return c.car(); }
		throw up( "expected a non-empty list: " + d );
	}

	/** {@return the numeric value of a decimal, hex-float, or rational scalar} */
	private static double num(Datum d)
	{
		if (!(d instanceof Atom a)) { throw up( "not a number: " + d ); }
		String s = a.value();
		int slash = s.indexOf('/');
		if (slash < 0) { return Double.parseDouble(s); }
		long n = Long.parseLong(s.substring(0, slash));
		long dv = Long.parseLong(s.substring(slash + 1));
		return (double) n / (double) dv;
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

	private static List<Ink> loadInk(Datum root)
	{
		List<Ink> brushKit = new ArrayList<>();
		for (Datum node : tagged("ink", root)) {
			Map<String,Datum> props = toAlist(node);
			Datum colorNode = props.get("color");
			float[] chan = new float[4];
			Color color;
			Walk colorIt = new Walk(colorNode);
			int i = 0;
			while (colorIt.hasNext()) {
				if (i >= 4) {
					throw up( "wrong arity" );
				}
				String value = ((Atom) colorIt.next()).value();
				chan[i++] = Float.parseFloat(value);
			}
			if (colorIt.tail() != Nil.INSTANCE) {
				throw up( "improper list (tail is: " + colorIt.tail() + ")" );
			}
			color = new Color(chan[0], chan[1], chan[2], chan[3]);
			brushKit.add(new Ink(color));
		}
		return brushKit;
	}

	private static Clock loadClock(Datum node)
	{
		Walk it = new Walk(node);
		int[] args = new int[3];
		int i = 0;
		while (it.hasNext()) {
			if (i >= 3) {
				throw up( "wrong arity" );
			}
			String value = ((Atom) it.next()).value();
			args[i++] = Integer.parseInt(value);
		}
		if (it.tail() != Nil.INSTANCE) {
			throw up( "improper list (tail is: " + it.tail() + ")" );
		}
		return new Clock(args[0], args[1], args[2]);
	}

	/*
	 * Both `transform' and `ink-transform' are absent on most strokes.
	 * When `transform' itself is missing but the stroke has a nested
	 * (substrokes (stroke ...) ...) -- the post-erase leftover fragment
	 * drawing.md describes -- that child usually still carries a real
	 * `transform', which is the one the whole (pre-erase) point list
	 * was actually meant to be placed with. `substrokesNode' is null
	 * when this fallback doesn't apply (e.g. for ink-transform).
	 */
	private static AffineTransform loadTransformOrIdentity(Datum node, Datum substrokesNode)
	{
		if (node != null) {
			return loadTransform(node);
		}
		if (substrokesNode != null) {
			for (Datum sub : tagged("stroke", substrokesNode)) {
				Datum childTransform = toAlist(sub).get("transform");
				if (childTransform != null) {
					return loadTransform(childTransform);
				}
			}
		}
		return new AffineTransform();
	}

	private static AffineTransform loadTransform(Datum node)
	{
		Walk it = new Walk(node);
		float[] args = new float[6];
		int i = 0;
		while (it.hasNext()) {
			if (i >= 6) {
				throw up( "wrong arity" );
			}
			String value = ((Atom) it.next()).value();
			args[i++] = Float.parseFloat(value);
		}
		if (i != 6) {
			throw up( "wrong arity" );
		}
		if (it.tail() != Nil.INSTANCE) {
			throw up( "improper list (tail is: " + it.tail() + ")" );
		}
		return new AffineTransform(args);
	}
}
