package org.rapidcow.trivnote.scripts;

import java.util.List;

/**
 * A single stroke, as a sequence of points.
 *
 * @param uuid   stroke identifier
 * @param points points in drawing order
 */
record Stroke(String uuid, List<Point> points) {}
