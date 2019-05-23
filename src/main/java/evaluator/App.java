package evaluator;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.transform.stream.StreamSource;


@Command(
    description = "Fetches HTML for the supplied URLs and applies the provided template",
    name = "template_eval",
    mixinStandardHelpOptions = true,
    version = "0.1")
public class App implements Callable<Void> {
  private static final Processor processor = new Processor(false);

  @Parameters(index = "0", description = "File containing sample URLs. One URL per line")
  private File urlFile;

  @Parameters(index = "1", description = "Template file")
  private File templateFile;

  @Parameters(index = "2", description = "Folder for HTML cache")
  private String htmlCacheFolderName;

  private HTMLCache htmlCache = null;

  public static void main(String[] args) {
    CommandLine.call(new App(), args);
  }

  @Override
  public Void call() throws Exception {
    //Initialize the on-disk HTML cache
    htmlCache = new HTMLCache(htmlCacheFolderName);

    Template template = loadTemplate(templateFile);
    List<String>
        rulesNames =
        template.rules.stream().map(rule -> rule.name).collect(Collectors.toList());
    rulesNames.add(0, "url");
    System.out.println(String.join("\t", rulesNames));
    try (Stream<String> lines = Files.lines(urlFile.toPath())) {
      lines.forEachOrdered(line -> {
        if (!line.trim().isEmpty()) {
          XdmNode node = fetchDocument(line);
          if (node != null) {
            List<String> results = applyTemplate(line, template, node);
            if (results != null) {
              System.out.println(String.join("\t", results));
            } else {
              System.err.println(
                  "URL " + line + " did not match template url regex: " + template.pattern);
            }
          }
        }
      });
    }
    return null;
  }

  public static XdmNode buildNode(byte[] html) throws SaxonApiException {
    processor.setConfigurationProperty(
        Feature.SOURCE_PARSER_CLASS, "org.ccil.cowan.tagsoup.Parser");
    processor.setConfigurationProperty(
        Feature.ENTITY_RESOLVER_CLASS, "evaluator.NoOpEntityResolver");
    return processor.newDocumentBuilder().build(
        new StreamSource(new ByteArrayInputStream(html)));
  }

  //Fetch the URL's HTML contents, either from HTML cache-on-disk, or from the internet.
  //Also, store a copy of the HTML contents on the cache, if it's enabled.
  //Returns the document (or null, in case of error).
  XdmNode fetchDocument(String url) {
    byte[] html = null;

    if (htmlCache.enabled()) {
      html = htmlCache.fetchDocument(url);
    }

    if (html == null) {
      try {
        html = innerFetch(url, true);
      } catch (MalformedURLException e) {
        System.err.println("URL: " + url + " is not a valid URL: " + e.getMessage());
        return null;
      } catch (IOException e) {
        System.err.println("Failed to fetch HTML (certificate validation on) from " + url + " : "
            + e.getMessage());
      }
      if (html == null) {
        try {
          html = innerFetch(url, false);
          System.err.println("Successfully fetched HTML from " + url
              + " with certificate validation off");
        } catch (IOException e) {
          System.err.println("Failed to fetch HTML (certificate validation off) from " + url + " : "
              + e.getMessage());
          return null;
        }
      }
    }

    try {
      return buildNode(html);
    } catch (SaxonApiException e) {
      System.err.println("Failed to parse HTML from " + url + " : "
          + e.getMessage());
      return null;
    }
  }

  private byte[] innerFetch(String url, boolean validateCertificates) throws IOException {
    Connection.Response response = Jsoup.connect(url)
        .userAgent(
            "Mozilla/5.0 (compatible; Pinterestbot/1.0; +http://www.pinterest.com/bot.html)")
        .validateTLSCertificates(validateCertificates)
        .method(Connection.Method.GET)
        .execute();
    byte[] html = response.bodyAsBytes();
    if (htmlCache.enabled()) {
      htmlCache.insertDocument(url, html);
    }
    return html;
  }

  List<String> applyTemplate(String url, Template template, XdmNode node) {
    List<String> results = null;

    if (template.urlMatch.test(url)) {
      results = new ArrayList<>();
      results.add(url);
      for (Rule rule : template.rules) {
        XdmValue result;
        try {
          rule.evaluator.setContextItem(node);
          result = rule.evaluator.evaluate();
        } catch (SaxonApiException e) {
          System.err.println("Failed to evaluate XPath " + rule.xPath +
              " on " + url + ":" + e.getMessage());
          continue;
        }
        if (result.size() == 0) {
          results.add("");
        } else if ("LIST_TEXT".equals(rule.outputFormat)) {
          results.add(StreamSupport.stream(result.spliterator(), false)
              .map(XdmItem::getStringValue).collect(Collectors.joining(",")));
        } else if ("TEXT".equals(rule.outputFormat)) {
          results.add(result.itemAt(0).getStringValue().replaceAll("[\\n\\t]", " "));
        }
      }
    }
    return results;
  }

  Template loadTemplate(File templateFile) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Template template = mapper.readValue(templateFile, Template.class);
    XPathCompiler compiler = processor.newXPathCompiler();
    compiler.declareNamespace("", "http://www.w3.org/1999/xhtml");
    compiler.setLanguageVersion("2.0");
    template.validateAndInitialize(compiler);
    return template;
  }
}
