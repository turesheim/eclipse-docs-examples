/*******************************************************************************
 * Copyright (c) 2015, 2016 Torkild U. Resheim
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Torkild U. Resheim - initial implementation
 *******************************************************************************/
package org.eclipse.mylyn.docs.examples;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class GenerateScreenshots {

	private static SWTWorkbenchBot bot;

	@BeforeClass
	public static void beforeClass() throws Exception {
		bot = new SWTWorkbenchBot();
	}

	@Test
	public void takeScreenshot() {
		// start Eclipse and switch to the Java perspective
		bot.viewByTitle("Welcome").close();
		bot.perspectiveByLabel("Java").activate();

		// take the screenshot
		Utilities.takeScreenshot(bot.activeShell().widget, "screenshot.png");
	}

}
