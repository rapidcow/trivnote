/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.rapidcow.trivnote.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.rapidcow.ai.trivnote.desktop.ExprPane;

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
		stage.setTitle("trivnote");
		stage.setScene(new Scene(new ExprPane(), 640, 480));
		stage.show();
	}

	public static void
	main(String[] args)
	{
		launch(args);
	}
}
