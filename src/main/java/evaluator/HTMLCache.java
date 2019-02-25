package evaluator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HTMLCache {
	
	private boolean cacheEnabled = true;
	private String cacheFolderName = null;
	private FileWriter cacheTable = null;
	
	//Initialize the HTML cache on disk. In case this fails, proceed without a cache.	
	public HTMLCache(String path) {
	  try {
		cacheFolderName = path.trim();
		if (cacheFolderName.charAt(cacheFolderName.length()-1) != '/') {
		  cacheFolderName = cacheFolderName.concat("/");
		}
		File cacheFolder = new File(cacheFolderName);
		cacheFolder.mkdirs();
		cacheFolder.setWritable(true);
		
		cacheTable = new FileWriter(cacheFolderName + ".table", true);
		
	  } catch (SecurityException e) {
		cacheEnabled = false;	
		System.err.println("WARNING: Failed to create Writable Folder " + cacheFolderName + " for HTML cache: " + e.getMessage() + ". Proceeding without Cache");  
	  } catch (IOException e) {
		cacheEnabled = false;
		System.err.println("WARNING: Failed to create Cache Table file " + cacheFolderName +".table :" + e.getMessage() + ". Proceeding without Cache");
	  }

	}
	
	
	public boolean enabled() { 
	  return cacheEnabled;
	}
	
	public Document fetchDocument(String url) {
	  Document document = null;
	  String encodedURL = encode(url);
    	  
      try {
    	File cacheFile = new File(encodedURL);        
    	document = Jsoup.parse(cacheFile, "UTF-8", url);
    	//System.out.println(url + " successfully found in the cache");    	
      } catch (IOException e) {  
    	//System.out.println(url + " not found in the cache");	    	
    	document = null;
      }
      
      return document;
	}
	
	public void insertDocument(String url, Document document) {
	  String encodedURL = encode(url);
	  
	  try {               
	    FileWriter writer = new FileWriter(encodedURL);
	    writer.write(document.html());
	    writer.flush();
	    writer.close();
	    
	    cacheTable.write(encodedURL + " <--> " + url + "\n");
	    cacheTable.flush();
	  } catch (IOException ioErr) {
	    System.err.println("WARNING: Failed to create cached copy for " + url + " : " + ioErr.getMessage());  
	  }
	}
	
	
	private String encode(String url) {
	  
	  String sha256hex = DigestUtils.sha256Hex(url); 
	  sha256hex = cacheFolderName.concat(sha256hex).concat(".") +  url.length();

	  //System.out.println ("The SHA-256 hash of " + url + " is : " + sha256hex);
	  return sha256hex;
	}
}