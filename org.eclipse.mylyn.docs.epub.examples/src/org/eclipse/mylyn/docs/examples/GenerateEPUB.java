package org.eclipse.mylyn.docs.examples;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.mylyn.docs.epub.core.EPUB;
import org.eclipse.mylyn.docs.epub.core.Publication;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.markdown.core.MarkdownLanguage;

import uk.ac.ed.ph.snuggletex.SerializationMethod;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;
import uk.ac.ed.ph.snuggletex.XMLStringOutputOptions;

public class GenerateEPUB {
	
	private static final File HTML_FILE = new File("loremipsum.html");
	
	private static final Pattern INLINE_EQUATION = Pattern.compile("\\$\\$?[^$]*\\$\\$?");
	
	public static void main(String[] args) {			

		try (
				// read MarkDown
				FileReader fr = new FileReader("loremipsum.md");
				// and output HTML
				FileWriter fw = new FileWriter(HTML_FILE)){
			
			// configure the MarkDown parser
			MarkupParser parser = new MarkupParser();
			parser.setMarkupLanguage(new MarkdownLanguage());
			HtmlDocumentBuilder builder = new HtmlDocumentBuilder(fw);
			parser.setBuilder(builder);
			parser.parse(fr, true);
			
			// convert any inline equations into MathML
			String html = Utilities.readFile(HTML_FILE, Charset.forName("UTF-8"));
			StringBuffer sb = new StringBuffer();
	        Matcher m = INLINE_EQUATION.matcher(html);
	        // for each equation
	        while (m.find()){
	        	// replace the LaTeX code with MathML
				m.appendReplacement(sb, laTeX2MathMl(m.group()));
	        }        
	        m.appendTail(sb);
	        Files.write(HTML_FILE.toPath(), sb.toString().getBytes(), StandardOpenOption.WRITE);
						
			// instantiate a new EPUB
			EPUB epub = new EPUB();
			Publication pub = Publication.getVersion2Instance();
			
			// include referenced resources is default false
			pub.setIncludeReferencedResources(true);
			pub.addTitle("EclipseCon Demo");
			pub.addSubject("EclipseCon Demo");
			
			// generate table of contents is default true
			pub.setGenerateToc(true);
			epub.add(pub);
			
			// add one chapter
			pub.addItem(HTML_FILE);
			
			// create the EPUB
			epub.pack(new File("loremipsum.epub"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
				
	}
	
	private static String laTeX2MathMl(String latex) throws IOException {
		
		// this is possibly a reference to a equation file
		if (latex.contains(".eqn")){
			String filename = latex.replaceAll("\\$","");
			File f = new File(filename);
			// if so load the contents of the file
			if (f.exists()){
				latex = "$$"+new String(Files.readAllBytes(f.toPath()))+"$$";
			}
		}
		
		// remove line breaks from within math expressions
		latex = latex.replace("<br></br>", "");
		latex = latex.replace("<br />", "");
		latex = latex.replace("<br/>", "");
		// back-convert html entities to utf-8
		String utf8latex = StringEscapeUtils.unescapeHtml(latex);

		SnuggleEngine engine = new SnuggleEngine();
		SnuggleSession session = engine.createSession();
		SnuggleInput input = new SnuggleInput(utf8latex);
		session.parseInput(input);

		XMLStringOutputOptions options = new XMLStringOutputOptions();
		options.setSerializationMethod(SerializationMethod.XML);
		options.setIndenting(true);
		options.setEncoding("UTF-8");
		options.setAddingMathSourceAnnotations(false);
		options.setUsingNamedEntities(true);
		return session.buildXMLString(options);
	}	
	
}
