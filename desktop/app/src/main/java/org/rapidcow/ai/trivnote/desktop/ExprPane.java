/*
 * No copyright
 */
package org.rapidcow.ai.trivnote.desktop;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import org.rapidcow.trivnote.libexpr.DataDumper;
import org.rapidcow.trivnote.libexpr.DataLoader;
import org.rapidcow.trivnote.libexpr.Datum;

/**
 * S-expression scratch pad: an editable source panel on top,
 * the {@link DataDumper} rendition of it below, and a row of
 * buttons in the bottom-right corner.
 *
 * <p>
 * The "Dark" toggle switches to a dark palette; it starts out
 * (and follows) the platform's color scheme.
 *
 * <p>
 * The action button cycles through three states:
 * <ul>
 * <li>"Parse" loads the source panel and dumps it into the
 *     display panel;
 * <li>"Devour" (after a successful parse, with the source
 *     untouched since) moves the display panel back into the
 *     source panel;
 * <li>":(" (after a failed parse) is disabled until the source
 *     panel is edited.
 * </ul>
 */
public
class
	ExprPane
extends
	BorderPane
{
	private enum State { PARSE, DEVOUR, FAILED }

	/**
	 * Set on the display panel while it shows a stack trace.
	 */
	private static final PseudoClass ERROR
		= PseudoClass.getPseudoClass("error");

	/**
	 * Set on the pane while the dark palette is in use.
	 */
	private static final PseudoClass DARK
		= PseudoClass.getPseudoClass("dark");

	private final TextArea source = new TextArea();
	private final TextArea display = new TextArea();
	private final Button action = new Button();
	private final ToggleButton dark = new ToggleButton("Dark");
	private final Button exit = new Button("Exit");

	private State state;

	/**
	 * Creates the pane with empty panels.
	 */
	public
	ExprPane()
	{
		getStylesheets().add(
				ExprPane.class.getResource("expr-pane.css").toExternalForm());
		getStyleClass().add("expr-pane");

		source.getStyleClass().add("source");
		display.getStyleClass().add("display");
		display.setEditable(false);
		exit.getStyleClass().add("exit");

		var panels = new SplitPane(source, display);
		panels.setOrientation(Orientation.VERTICAL);
		setCenter(panels);

		var buttons = new HBox(8, dark, action, exit);
		buttons.setAlignment(Pos.CENTER_RIGHT);
		buttons.setPadding(new Insets(8));
		setBottom(buttons);

		action.setOnAction(e -> act());
		exit.setOnAction(e -> Platform.exit());
		dark.selectedProperty().addListener(
				(obs, was, now) -> pseudoClassStateChanged(DARK, now));
		var scheme = Platform.getPreferences().colorSchemeProperty();
		scheme.addListener(
				(obs, was, now) -> dark.setSelected(now == ColorScheme.DARK));
		dark.setSelected(scheme.get() == ColorScheme.DARK);
		pseudoClassStateChanged(DARK, dark.isSelected());
		source.textProperty().addListener((obs, was, now) -> enter(State.PARSE));
		enter(State.PARSE);
	}

	private void
	act()
	{
		switch (state) {
		case PARSE -> parse();
		case DEVOUR -> devour();
		case FAILED -> { /* button is disabled */ }
		}
	}

	private void
	parse()
	{
		var out = new StringWriter();
		try {
			var dumper = new DataDumper(out);
			var loader = new DataLoader(new StringReader(source.getText()));
			for (Datum d : loader) {
				dumper.writeDatum(d);
				out.write('\n');
			}
			if (loader.lastIOException() != null) {
				throw loader.lastIOException();
			}
		} catch (IOException | RuntimeException e) {
			var trace = new StringWriter();
			e.printStackTrace(new PrintWriter(trace));
			show(trace.toString(), true);
			enter(State.FAILED);
			return;
		}
		show(out.toString(), false);
		enter(State.DEVOUR);
	}

	private void
	devour()
	{
		String text = display.getText();
		show("", false);
		source.setText(text);
		/* The source listener won't fire if text was unchanged. */
		enter(State.PARSE);
	}

	private void
	show(String text, boolean error)
	{
		display.setText(text);
		display.pseudoClassStateChanged(ERROR, error);
	}

	private void
	enter(State next)
	{
		state = next;
		switch (next) {
		case PARSE -> action.setText("Parse");
		case DEVOUR -> action.setText("Devour");
		case FAILED -> action.setText(":(");
		}
		action.setDisable(next == State.FAILED);
	}
}
