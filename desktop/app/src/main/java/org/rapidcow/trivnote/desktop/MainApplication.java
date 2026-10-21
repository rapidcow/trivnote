/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.rapidcow.trivnote.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Entry point of the trivnote desktop application.
 */
public class
MainApplication
extends
	Application
{
	@Override
	public void
	start(Stage stage)
	{
		var label = new Label("Hello, JavaFX "
				+ System.getProperty("javafx.version")
				+ " on Java "
				+ System.getProperty("java.version")
				+ "!");
		stage.setTitle("trivnote");
		stage.setScene(new Scene(new StackPane(label), 400, 200));
		stage.show();
	}

	public static void
	main(String[] args)
	{
		launch(args);
	}
}
