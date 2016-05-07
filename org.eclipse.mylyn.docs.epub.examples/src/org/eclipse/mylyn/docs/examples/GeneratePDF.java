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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.XslfoDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.builder.XslfoDocumentBuilder.Configuration;
import org.eclipse.mylyn.wikitext.core.parser.outline.OutlineItem;
import org.eclipse.mylyn.wikitext.core.parser.outline.OutlineParser;
import org.eclipse.mylyn.wikitext.markdown.core.MarkdownLanguage;
import org.scilab.forge.jlatexmath.DefaultTeXFont;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import org.scilab.forge.jlatexmath.cyrillic.CyrillicRegistration;
import org.scilab.forge.jlatexmath.greek.GreekRegistration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class GeneratePDF {
	
	private static final File XSLFO_FILE = new File("loremipsum.fo");
	
	private static final Pattern EQUATION = Pattern.compile("\\$\\$?[^$]*\\$\\$?");

	public static void main(String[] args) {			

		try (	// read MarkDown
				FileReader fr = new FileReader("loremipsum.md");
				// output XSL:FO
				FileWriter fw = new FileWriter("loremipsum.fo");
				// and finally PDF
				OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("loremipsum.pdf")))) {
			
			// generate XSL:FO file from the MarkDown
			MarkupParser parser = new MarkupParser();
			parser.setMarkupLanguage(new MarkdownLanguage());
			XslfoDocumentBuilder builder = new XslfoDocumentBuilder(fw);

			Configuration c = new Configuration();
			// create a cover page
			c.setTitle("Lorem impsum");
			c.setVersion("Version 1.0");
			c.setAuthor("Nomen Nescio");
			c.setDate(LocalDate.now().toString());
			// and a footer
			c.setCopyright("Copyright 2015-2016, Nomen Nescio");
			builder.setConfiguration(c);

			// the input file must be parsed to get create a table of contents
			OutlineItem op = new OutlineParser(new MarkdownLanguage())
					.parse(Utilities.readFile(new File("loremipsum.md"), Charset.forName("utf-8")));
			builder.setOutline(op);

			// build the XSL:FO file
			parser.setBuilder(builder);
			parser.parse(fr, true);

			// convert any inline equations in the HTML into SVG
			String html = Utilities.readFile(XSLFO_FILE, Charset.forName("UTF-8"));
			StringBuffer sb = new StringBuffer();
			Matcher m = EQUATION.matcher(html);

			// for each equation
			while (m.find()) {
				// replace the LaTeX code with MathML
				m.appendReplacement(sb, laTeX2Svg(m.group()));
			}
			m.appendTail(sb);
			Files.write(XSLFO_FILE.toPath(), sb.toString().getBytes(), StandardOpenOption.WRITE);

			// create a new Fop using the given configuration
			FopFactory fopFactory = FopFactory.newInstance(new File("fop.xconf"));
			Fop fop = fopFactory.newFop("application/pdf", out);

			// create a new transformer
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();

			// generate the PDF from the XSL:FO
			Source src = new StreamSource(new File("loremipsum.fo"));
			Result res = new SAXResult(fop.getDefaultHandler());
			transformer.transform(src, res);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private static String laTeX2Svg(String latex) throws IOException {

		// this is possibly a reference to a equation file
		if (latex.contains(".eqn")) {
			String filename = latex.replaceAll("\\$", "");
			File f = new File(filename);
			// if so load the contents of the file into the equation variable
			if (f.exists()) {
				latex = "$$" + new String(Files.readAllBytes(f.toPath())) + "$$";
			}
		}

		DefaultTeXFont.registerAlphabet(new CyrillicRegistration());
		DefaultTeXFont.registerAlphabet(new GreekRegistration());

		// declare that we want to create SVG
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);
		SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);

		// create an SVG canvas to draw on
		SVGGraphics2D g2 = new SVGGraphics2D(ctx, true);
		TeXFormula formula = new TeXFormula(latex);

		// do the actual drawing
		TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 15);
		icon.paintIcon(null, g2, 0, 0);

		// and stream it out to a string
		StringWriter svgWriter = new StringWriter();
		g2.stream(svgWriter, true);
		String svg = svgWriter.toString();

		// kludge to remove xml and doctype declaration
		svg = svg.substring(svg.indexOf('\n') + 1);
		svg = svg.substring(svg.indexOf('\n') + 1);
		svg = svg.substring(svg.indexOf('\n') + 1);

		// declare that this object is not XSL:FO and embed it
		StringBuilder sb = new StringBuilder();
		sb.append("<instream-foreign-object>");
		sb.append(svg);
		sb.append("</instream-foreign-object>");
		
		return sb.toString();
	}
	
}
