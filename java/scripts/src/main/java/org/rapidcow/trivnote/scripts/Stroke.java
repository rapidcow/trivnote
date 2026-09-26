package org.rapidcow.trivnote.scripts;

import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * A single stroke, as a sequence of points.
 *
 * @param clock  sort identifier
 * @param uuid   stroke identifier
 * @param brush  brush properties
 * @param affineMap affine map to apply
 * @param points points in drawing order
 */
record Stroke(
    Clock clock,
    String uuid,
    Ink brush,
    AffineTransform affineMap,
    List<Point> points) {}
