package org.eclipse.mylyn.docs.examples;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Utilities {

	static final int RADIUS = 16;

	public static void takeScreenshot(final Shell shell, String filename) {
		shell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				// Grab a screenshot of the dialog shell
				final Rectangle b = shell.getBounds();
				final Image screenshot = new Image(shell.getDisplay(), b.width, b.height);
				GC gc = new GC(shell.getDisplay());
				gc.copyArea(screenshot, b.x, b.y);
	
				// Create drop shadow image
				final Image image = new Image(shell.getDisplay(), b.width + RADIUS, b.height + RADIUS);
				GC gc2 = new GC(image);
				fillRoundRectangleDropShadow(gc2, image.getBounds(), RADIUS);
				gc2.drawImage(screenshot, RADIUS / 2, RADIUS / 2);
	
				File file = new File(filename);
				ImageLoader loader = new ImageLoader();
				if (file.exists()) {
					loader.load(file.getAbsolutePath());
					Image original = new Image(shell.getDisplay(), file.getAbsolutePath());
					if (!original.getImageData().equals(image.getImageData())) {
						loader.data = new ImageData[] { image.getImageData() };
						loader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
					}
				} else {
					loader.data = new ImageData[] { image.getImageData() };
					loader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
				}
				gc.dispose();
			}
		});
	}

	public static void fillRoundRectangleDropShadow(GC gc, Rectangle bounds, int radius) {
		gc.setAdvanced(true);
		gc.setAntialias(SWT.ON);
		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		gc.setAlpha(0x8f / radius);
		for (int i = 0; i < radius; i++) {
			Rectangle shadowBounds = new Rectangle(bounds.x + i, bounds.y + i, bounds.width - (i * 2),
					bounds.height - (i * 2));
			gc.fillRoundRectangle(shadowBounds.x, shadowBounds.y, shadowBounds.width, shadowBounds.height, radius * 2,
					radius * 2);
		}
		gc.setAlpha(0xff);
	}

	static String readFile(File file, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(file.toPath());
		return new String(encoded, encoding);
	}
}
