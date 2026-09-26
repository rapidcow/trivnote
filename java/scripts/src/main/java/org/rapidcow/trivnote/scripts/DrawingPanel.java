package org.rapidcow.trivnote.scripts;

import java.util.List;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JPanel;

class DrawingPanel extends JPanel
{
	private List<Stroke> art;

	DrawingPanel(List<Stroke> strokes)
	{
		art = strokes;
	}

	@Override protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);

		Graphics2D canvas = (Graphics2D) graphics.create();
		try {
			paintOnCanvas(canvas);
		}
		finally {
			canvas.dispose();
		}
	}

	private void paintOnCanvas(Graphics2D canvas)
	{
		canvas.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON
		);
		canvas.setStroke(new BasicStroke(
			2.5f,  /* Full width */
			BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND
		));

		for (Stroke stroke : art) {
			canvas.setColor(stroke.brush().color());
			Path2D.Double path = new Path2D.Double();
			boolean lifted = true;

			for (Point point : stroke.points()) {
				if (lifted) {
					path.moveTo(point.x(), point.y());
					lifted = false;
				}
				else {
					path.lineTo(point.x(), point.y());
				}
			}

			canvas.draw(path);
		}
	}
}
