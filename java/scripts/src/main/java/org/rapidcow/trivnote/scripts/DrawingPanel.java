package org.rapidcow.trivnote.scripts;

import java.util.List;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import javax.swing.JPanel;
import javax.swing.Timer;

class DrawingPanel extends JPanel
{
	private List<Stroke> art;
	private int revealed = 0;

	DrawingPanel(List<Stroke> strokes)
	{
		art = strokes;
	}

	/** Reveals one stroke every {@code millisPerStroke}, starting immediately. */
	void animate(int millisPerStroke)
	{
		Timer timer = new Timer(millisPerStroke, null);
		timer.setInitialDelay(0);
		timer.addActionListener(e -> {
			revealed++;
			repaint();
			if (revealed >= art.size()) {
				timer.stop();
			}
		});
		timer.start();
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

		String uuid = null;
		for (int i = 0; i < revealed && i < art.size(); i++) {
			Stroke stroke = art.get(i);
			uuid = stroke.uuid();
			canvas.setColor(stroke.brush().color());
			canvas.setTransform(stroke.affineMap());
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

		if (uuid != null) {
			canvas.setTransform(new AffineTransform());  /* back to screen space */
			canvas.setColor(Color.BLACK);
			canvas.drawString(uuid, 8, 16);
		}
	}
}
