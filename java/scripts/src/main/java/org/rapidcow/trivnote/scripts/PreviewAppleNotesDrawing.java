package org.rapidcow.trivnote.scripts;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.rapidcow.trivnote.libexpr.Symbol;

public class PreviewAppleNotesDrawing
{
	/*
	 * Shared by the loader and getSymbol, so every symbol we
	 * look up is the very instance the loader produced.  Only
	 * ever touched from the main thread, hence a plain HashMap.
	 */
	private static final Map<String,Symbol> SYMBOLS = new HashMap<>();

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

	/** {@return the symbol for {@code name}, from the same table the loader uses} */
	private static Symbol getSymbol(String name)
	{
		return SYMBOLS.computeIfAbsent(name, Symbol::of);
	}

	public static void main(String[] args) throws IOException
	{
		Path path;
		Datum root;

		if (args.length != 1) { throw up( "usage: PreviewAppleNotesDrawing FILE" ); }

		path = Path.of(args[0]);
		try (BufferedReader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			DataLoader loader = new DataLoader(in).useSymbolCache(SYMBOLS)
				.useLengthLimit(0xffffL).useDepthLimit(0xfL);
			root = loader.readDatum();
			if (loader.hasNext()) { throw up( "trailing garbage" ); }
			if (loader.lastIOException() != null) { throw up( "I/O error", loader.lastIOException() ); }
		}

		Datum rK, rV;
		Map<Datum,Datum> drawing;

		if (root instanceof Cons r) {
			rK = r.car();
			rV = r.cdr();
		} else {
			throw up( "root node not a cons cell: " + root );
		}

		if (!( getSymbol("drawing").equals(rK) )) {
			throw up( "bad root key: " + root );
		}

		drawing = Datum.asAlist(rV).requireUniqueKeys();

		List<Ink> brushKit = loadInk(drawing.get(getSymbol("inks")));
		List<Stroke> strokes = loadStrokes(brushKit, drawing.get(getSymbol("strokes")));
		System.out.println(strokes.size()
			+ " stroke"
			+ (strokes.size() == 1 ? "" : "s")
			+ " loaded");
		System.out.println("Symbol table ("
			+ SYMBOLS.size() + "): "
			+ SYMBOLS.values());

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
	 * a plain list of (stroke ...) tagged forms, so it can't be
	 * read as an alist directly here (every entry shares the same
	 * "stroke" tag, which requireUniqueKeys would reject).
	 */
	private static List<Stroke> loadStrokes(List<Ink> kit, Datum strokesNode)
	{
		List<Stroke> strokes = new ArrayList<>();
		for (Datum node : tagged("stroke", strokesNode)) {
			Map<Datum,Datum> fields = Datum.asAlist(node).requireUniqueKeys();
			Clock sortId = loadClock(fields.get(getSymbol("sort-id")));
			String uuid = Datum.value(first(fields.get(getSymbol("uuid"))));
			Ink ink = kit.get(Integer.parseInt(Datum.value(first(fields.get(getSymbol("ink-index"))))));
			AffineTransform map = loadTransformOrIdentity(fields.get(getSymbol("transform")), fields.get(getSymbol("substrokes")));
			AffineTransform inkMap = loadTransformOrIdentity(fields.get(getSymbol("ink-transform")), null);
			strokes.add(new Stroke(sortId, uuid, ink, map, inkMap, loadPoints(fields.get(getSymbol("points")))));
		}
		strokes.sort(Comparator.comparing(Stroke::clock));
		return strokes;
	}

	/*
	 * `pointsNode' is the cdr of a (points ...) entry: a plain list
	 * whose elements are themselves alists (each already shaped as
	 * ((point X Y) (altitude . R) (force . R) (radius . R) ...)),
	 * so asAlist applies directly to each element.
	 */
	private static List<Point> loadPoints(Datum pointsNode)
	{
		Symbol pointKey = getSymbol("point");
		Symbol radiusKey = getSymbol("radius");
		List<Point> points = new ArrayList<>();
		for (Datum node : Datum.asList(pointsNode)) {
			Map<Datum,Datum> fields = Datum.asAlist(node).requireUniqueKeys();
			List<Datum> xy = Datum.asList(fields.get(pointKey));
			if (xy.size() != 2) {
				throw up( "wrong arity" );
			}
			double x = num(xy.get(0));
			double y = num(xy.get(1));
			/* radius is missing on some points (e.g. dry taps) */
			Datum radius = fields.get(radiusKey);
			points.add(new Point(x, y, radius != null ? num(radius) : 0.0));
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
		Symbol wantTag = getSymbol(tag);
		for (Datum elt : Datum.asList(node)) {
			if (!(elt instanceof Cons cp) || !wantTag.equals(cp.car())) {
				throw up( "expected a (" + tag + " ...) form: " + elt );
			}
			rest.add(cp.cdr());
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

	private static List<Ink> loadInk(Datum root)
	{
		List<Ink> brushKit = new ArrayList<>();
		for (Datum node : tagged("ink", root)) {
			Map<Datum,Datum> props = Datum.asAlist(node).requireUniqueKeys();
			List<Datum> colorList = Datum.asList(props.get(getSymbol("color")));
			float[] chan = new float[4];
			int i = 0;
			if (colorList.size() > 4) {
				throw up( "wrong arity" );
			}
			for (Datum c : colorList) {
				chan[i++] = Float.parseFloat(Datum.value(c));
			}
			brushKit.add(new Ink(new Color(chan[0], chan[1], chan[2], chan[3])));
		}
		return brushKit;
	}

	private static Clock loadClock(Datum node)
	{
		List<Datum> list = Datum.asList(node);
		int[] args = new int[3];
		int i = 0;
		if (list.size() > 3) {
			throw up( "wrong arity" );
		}
		for (Datum d : list) {
			args[i++] = Integer.parseInt(Datum.value(d));
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
				Datum childTransform = Datum.asAlist(sub).requireUniqueKeys().get(getSymbol("transform"));
				if (childTransform != null) {
					return loadTransform(childTransform);
				}
			}
		}
		return new AffineTransform();
	}

	private static AffineTransform loadTransform(Datum node)
	{
		List<Datum> list = Datum.asList(node);
		float[] args = new float[6];
		int i = 0;
		if (list.size() != 6) {
			throw up( "wrong arity" );
		}
		for (Datum d : list) {
			args[i++] = Float.parseFloat(Datum.value(d));
		}
		return new AffineTransform(args);
	}
}
