package evaluator;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import picocli.CommandLine;
import us.codecraft.xsoup.XElements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base32;

@Command(
    description = "Fetches HTML for the supplied URLs and applies the provided template",
    name = "template_eval",
    mixinStandardHelpOptions = true,
    version = "0.1")
public class App implements Callable<Void>  {
  @Parameters(index = "0", description = "File containing sample URLs. One URL per line")
  private File urlFile;

  @Parameters(index = "1", description = "Template file")
  private File templateFile;
  
  @Parameters(index = "2", description = "Folder for URL cache")
  private String URLCacheFolderName;
  
  private boolean URLCacheEnabled = true;
  private File URLCache;

  public static void main(String[] args) {
    CommandLine.call(new App(), args);
  }
 
  @Override
  public Void call() throws Exception {    
	try {
	  URLCacheFolderName = URLCacheFolderName.trim();
	  if (URLCacheFolderName.charAt(URLCacheFolderName.length()-1) != '/') {
		  URLCacheFolderName = URLCacheFolderName.concat("/");
	  }
	  URLCache = new File(URLCacheFolderName);
	  URLCache.mkdirs();
	  URLCache.setWritable(true);
	} catch (SecurityException e) {
	  URLCacheEnabled = false;	
      System.err.println("WARNING: Failed to create Writable URL Cache Folder " + URLCacheFolderName + ": " + e.getMessage() + ". Proceeding without Cache");  
	}
	    
    Template template = loadTemplate(templateFile);
    List<String> rulesNames = template.rules.stream().map(rule -> rule.name).collect(Collectors.toList());
    rulesNames.add(0, "url");
    System.out.println(String.join("\t", rulesNames));
    try (Stream<String> lines = Files.lines(urlFile.toPath())) {
      lines.forEachOrdered(line -> {
        if(!line.trim().isEmpty()) {        	
          try {
        	Document document;
        	
        	if (URLCacheEnabled) {
              Base32 base32 = new Base32();  
        	  String encodedURL = new String(base32.encode(line.getBytes()));
        	  encodedURL = URLCacheFolderName.concat(encodedURL);
        	  
        	  try {
        	    File cacheFile = new File(encodedURL);        
        		document = Jsoup.parse(cacheFile, "UTF-8", line);
        		//System.out.println(line + " successfully found in the cache");
              } catch (IOException e) {  
        		//System.out.println(line + " not found in the cache");	
        		document = Jsoup.connect(line)
        				   .userAgent("Mozilla/5.0 (compatible; Pinterestbot/1.0; +http://www.pinterest.com/bot.html)")
        				   .get();

        		try {               
        		  FileWriter writer = new FileWriter(encodedURL);
        		  writer.write(document.html());
        		  writer.flush();
        		  writer.close();
        		} catch (IOException ioErr) {
        		  System.err.println("WARNING: Failed to create cached copy for " + line+ " : " + ioErr.getMessage());  
        		}
        	  }
        	} else {
        	  document = Jsoup.connect(line)
        			  	.userAgent("Mozilla/5.0 (compatible; Pinterestbot/1.0; +http://www.pinterest.com/bot.html)")
        			  	.get();
            
        	}
            List<String> results = applyTemplate(line, template, document);
            if (results != null) {
              System.out.println(String.join("\t", results));
            } else {
              System.err.println(
                "URL " + line + " did not match template url regex: " + template.pattern);
            }
        	
          } catch (MalformedURLException e) {
            System.err.println("URL: " + line + " is not a valid URL: " + e.getMessage());
          } catch (IOException e) {
            System.err.println("Failed to fetch HTML from " + line + " : " + e.getMessage());
          }
      }});
    }
    return null;
  }

  List<String> applyTemplate(String url, Template template, Document document) {
    List<String> results = null;
    if (template.urlMatch.test(url)) {
      results = new ArrayList<>();
      results.add(url);
      for (Rule rule : template.rules) {
        XElements elements = rule.evaluator.evaluate(document);
        List<String> values = elements.list();
        if (values.size() > 0) {
          results.add(String.join(", ", values));
        } else {
          results.add("");
        }
      }
    }
    return results;
  }

  Template loadTemplate(File templateFile) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Template template = mapper.readValue(templateFile, Template.class);
    template.validateAndInitialize();
    return template;
  }
}
