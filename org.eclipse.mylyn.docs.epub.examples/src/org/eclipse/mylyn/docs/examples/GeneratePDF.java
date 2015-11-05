package org.eclipse.mylyn.docs.examples;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.XslfoDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.builder.XslfoDocumentBuilder.Configuration;
import org.eclipse.mylyn.wikitext.core.parser.outline.OutlineItem;
import org.eclipse.mylyn.wikitext.core.parser.outline.OutlineParser;
import org.eclipse.mylyn.wikitext.markdown.core.MarkdownLanguage;

public class GeneratePDF {
	
	public static void main(String[] args) {			

		try (
				FileReader fr = new FileReader("loremipsum.md");
				FileWriter fw = new FileWriter("loremipsum.fo");
				OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("loremipsum.pdf")))) {
			
			// generate XSL:FO file from the MarkDown
			MarkupParser parser = new MarkupParser();
			parser.setMarkupLanguage(new MarkdownLanguage());
			XslfoDocumentBuilder builder = new XslfoDocumentBuilder(fw);
			
			// create a cover page
			Configuration c = new Configuration();
			c.setTitle("Lorem markdownum");
			c.setVersion("Version 1.0");
			c.setAuthor("Nomen Nescio");
			c.setDate(LocalDate.now().toString());
			// and a footer
			c.setCopyright("Copyright 2015, Jasper Van der Jeugt");
			
			builder.setConfiguration(c);

			// the input file must be parsed to get create a table of contents 
			OutlineItem op = new OutlineParser(new MarkdownLanguage())
					.parse(Utilities.readFile(new File("loremipsum.md"), Charset.forName("utf-8")));
			builder.setOutline(op);
			
			// build the XSL:FO file 
			parser.setBuilder(builder);
			parser.parse(fr, true);
			
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

		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
