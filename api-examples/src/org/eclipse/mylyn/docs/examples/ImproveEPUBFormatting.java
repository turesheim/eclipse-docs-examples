/*******************************************************************************
 * Copyright © 2015-2017 Torkild U. Resheim
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Torkild U. Resheim - initial implementation
 *******************************************************************************/
package org.eclipse.mylyn.docs.examples;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.mylyn.docs.epub.core.EPUB;
import org.eclipse.mylyn.docs.epub.core.ILogger;
import org.eclipse.mylyn.docs.epub.core.Publication;
import org.eclipse.mylyn.docs.epub.internal.EPUBFileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * This code demonstrates how the Eclipse EPUB API can be used to improve the
 * formatting of a published book. When executed it will examine all the code
 * blocks in the book and apply basic keyword coloring in addition to changing
 * the font to a more readable monospace type.
 * 
 * The book used is https://www.packtpub.com/application-development/advanced-eclipse-plug-development-raw
 * 
 * @author Torkild Ulvøy Resheim, Itema AS
 */
public class ImproveEPUBFormatting {
	
	// a logger is not required, but it helps
	static EPUB epub = new EPUB(new ILogger() {

		@Override
		public void log(String message) {
			System.out.println(message);
		}

		@Override
		public void log(String message, Severity severity) {
			System.out.println(message);
		}

	});

	/** A list of all Java keywords we want colored */
	static List<String> KEYWORDS = Arrays.asList("abstract", "continue", "for", "new", "switch", "assert", "default",
			"goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double",
			"implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
			"instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final",
			"interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
			"native", "super", "while", "true", "false", "null");

	public static void main(String[] args) {
		try {
			// assign a working folder
			File workFolder = new File("work");
			
			// clean up from last run
			try {
				FileUtils.deleteQuietly(workFolder);
				Files.delete(Paths.get("new.epub"));
			} catch (IOException e1) { /* no worries */ }

			// first unpack the EPUB file we have
			epub.unpack(new File("Mastering Eclipse Plug-in Development.epub"), workFolder);
			
			// make note of the CSS file
			File cssFile = new File(workFolder, "OEBPS/epub.css");

			// delete the old CSS file
			cssFile.delete();

			// then copy in the modified CSS
			EPUBFileUtil.copy(new File("assets/epub.css"), cssFile);

			// find all the chapters, we know they start with "ch" 
			File oebps = new File(workFolder, "OEBPS");
			String[] list = oebps.list(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("ch") && name.endsWith(".html");
				}
				
			});
			
			// and fix the code formatting
			for (String string : list) {
				File in = new File(oebps, string);
				improveCodeFormatting(in);
			}

			// add the Ubuntu monospace font
			Publication publication = epub.getOPSPublications().get(0);
			publication.addSubject("Demo");			
			publication.addItem(new File("assets/UbuntuMono-B.ttf"));
			publication.addItem(new File("assets/UbuntuMono-BI.ttf"));
			publication.addItem(new File("assets/UbuntuMono-R.ttf"));
			publication.addItem(new File("assets/UbuntuMono-RI.ttf"));

			// and lastly we pack the EPUB again
			epub.pack(new File("new.epub"), workFolder);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void improveCodeFormatting(File in) throws IOException {
		String contents = new String(Files.readAllBytes(in.toPath())); 
		
		// parse the file contents as XML and put it into a DOM
		Document parse = Jsoup.parse(contents, "UTF-8", Parser.xmlParser());

		// obtain all the code sections we want to format
		Elements select = parse.select("pre[class=programlisting]");
		for (Element element : select) {
			// get to the actual code
			String text = element.html();
		
			// try to avoid xml and other code already formatted - we're not
			// able to handle that in this example
			if (!text.contains("<plugin>") && !text.contains("<span")) {
				// format all keywords by adding the "keyword" class
				String code = KEYWORDS
						.stream()
						.reduce(text,
						(str, keyword) -> str.replaceAll(
								keyword+"(\\s|\\(|\\{|\\))", 
								"<code class=\"keyword\">" + keyword + "</code>$1"));
				// replace the code with the colored version
				element.html(code);
			}
		}
		
		// write out the modified code
		FileWriter fw = new FileWriter(in);
		fw.write(parse.outerHtml());
		fw.flush();
		fw.close();
	}
}
