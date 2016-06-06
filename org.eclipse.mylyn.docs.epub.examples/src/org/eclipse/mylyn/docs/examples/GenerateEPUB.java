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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.mylyn.docs.epub.core.EPUB;
import org.eclipse.mylyn.docs.epub.core.ILogger;
import org.eclipse.mylyn.docs.epub.core.Publication;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.markdown.core.MarkdownLanguage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import uk.ac.ed.ph.snuggletex.SerializationMethod;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;
import uk.ac.ed.ph.snuggletex.XMLStringOutputOptions;

public class GenerateEPUB {
		
	// matches '$$ ... $$' and '$ ... $'
	private static final Pattern EQUATION = Pattern.compile("\\$\\$?[^$]*\\$\\$?");
	
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

	public static void main(String[] args) {
		
		// clean up from last run
		try {
			Files.delete(Paths.get("loremipsum.html"));
			Files.delete(Paths.get("loremipsum.epub"));
		} catch (IOException e1) { /* no worries */ }

		try (	// read MarkDown
				FileReader fr = new FileReader("loremipsum.md");
				// and output HTML
				Writer fw = Files.newBufferedWriter(Paths.get("loremipsum.html"), StandardOpenOption.CREATE)){
			
			// generate HTML from markdown
			MarkupParser parser = new MarkupParser();
			parser.setMarkupLanguage(new MarkdownLanguage());
			HtmlDocumentBuilder builder = new HtmlDocumentBuilder(fw);
			parser.setBuilder(builder);
			parser.parse(fr, true);

			// convert any inline equations in the HTML into MathML
			String html  = new String(Files.readAllBytes(Paths.get("loremipsum.html")));
			StringBuffer sb = new StringBuffer();
			Matcher m = EQUATION.matcher(html);

			// for each equation
			while (m.find()) {
				// replace the LaTeX code with MathML
				m.appendReplacement(sb, laTeX2MathMl(m.group()));
			}
			m.appendTail(sb);
			
			// EPUB 2.0 can only handle embedded SVG so we find all referenced
			// SVG files and replace the reference with the actual SVG code
			Document parse = Jsoup.parse(sb.toString(), "UTF-8", Parser.xmlParser());

			Elements select = parse.select("img");
			for (Element element : select) {
				String attr = element.attr("src");
				if (attr.endsWith(".svg")){
					byte[] svg = Files.readAllBytes(Paths.get(attr));
					element.html(new String(svg));					
				}
			}
			
			
			// write back the modified HTML-file
			Files.write(Paths.get("loremipsum.html"), sb.toString().getBytes(), StandardOpenOption.WRITE);

			// instantiate a new EPUB version 2 publication
			Publication pub = Publication.getVersion2Instance();

			// include referenced resources (default is false)
			pub.setIncludeReferencedResources(true);
			
			// title and subject is required
			pub.addTitle("EclipseCon Demo");
			pub.addSubject("EclipseCon Demo");

			// generate table of contents (default is true)
			pub.setGenerateToc(true);
			epub.add(pub);

			// add one chapter
			pub.addItem(Paths.get("loremipsum.html").toFile());

			// create the EPUB
			epub.pack(new File("loremipsum.epub"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
				
	}
	
	private static String laTeX2MathMl(String latex) throws IOException {
		
		// this is possibly a reference to a equation file
		if (latex.contains(".eqn")) {
			String filename = latex.replaceAll("\\$", "");
			File f = new File(filename);
			// if so load the contents of the file into the equation variable
			if (f.exists()) {
				latex = "$$" + new String(Files.readAllBytes(f.toPath())) + "$$";
			}
		}

		// remove line breaks from within math expressions
		latex = latex.replace("<br></br>", "");
		latex = latex.replace("<br />", "");
		latex = latex.replace("<br/>", "");

		// convert html entities to utf-8
		String utf8latex = StringEscapeUtils.unescapeHtml(latex);

		// parse the LaTeX expression
		SnuggleEngine engine = new SnuggleEngine();
		SnuggleSession session = engine.createSession();
		SnuggleInput input = new SnuggleInput(utf8latex);
		session.parseInput(input);

		// and output MathML
		XMLStringOutputOptions options = new XMLStringOutputOptions();
		options.setSerializationMethod(SerializationMethod.XML);
		options.setIndenting(true);
		options.setEncoding("UTF-8");
		options.setAddingMathSourceAnnotations(false);
		options.setUsingNamedEntities(true);
		
		return session.buildXMLString(options);
	}	
	
}
