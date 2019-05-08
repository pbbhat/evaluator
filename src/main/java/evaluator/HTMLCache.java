package evaluator;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class HTMLCache {

  private boolean cacheEnabled = true;
  private String cacheFolderName = null;
  private FileWriter cacheTable = null;

  //Initialize the HTML cache on disk. In case this fails, proceed without a cache.
  public HTMLCache(String path) {
    try {
      cacheFolderName = path.trim();
      if (cacheFolderName.charAt(cacheFolderName.length() - 1) != '/') {
        cacheFolderName = cacheFolderName.concat("/");
      }
      File cacheFolder = new File(cacheFolderName);
      cacheFolder.mkdirs();
      cacheFolder.setWritable(true);

      cacheTable = new FileWriter(cacheFolderName + ".table", true);

    } catch (SecurityException e) {
      cacheEnabled = false;
      System.err.println(
          "WARNING: Failed to create Writable Folder " + cacheFolderName + " for HTML cache: " + e
              .getMessage() + ". Proceeding without Cache");
    } catch (IOException e) {
      cacheEnabled = false;
      System.err.println(
          "WARNING: Failed to create Cache Table file " + cacheFolderName + ".table :" + e
              .getMessage() + ". Proceeding without Cache");
    }

  }


  public boolean enabled() {
    return cacheEnabled;
  }

  public byte[] fetchDocument(String url) {
    byte[] html = null;
    String encodedURL = encode(url);
    File cacheFile = new File(encodedURL);

    try {
      html = Files.readAllBytes(cacheFile.toPath());
      //System.out.println(url + " successfully found in the cache");
    } catch (IOException e) {
      //System.out.println(url + " not found in the cache");
    }

    return html;
  }

  public void insertDocument(String url, byte[] html) {
    String encodedURL = encode(url);
    File cacheFile = new File(encodedURL);

    try {
      Files.write(cacheFile.toPath(), html);
      cacheTable.write(encodedURL + " <--> " + url + "\n");
      cacheTable.flush();
    } catch (IOException ioErr) {
      System.err
          .println("WARNING: Failed to create cached copy for " + url + " : " + ioErr.getMessage());
    }
  }


  private String encode(String url) {

    String sha256hex = DigestUtils.sha256Hex(url);
    sha256hex = cacheFolderName.concat(sha256hex).concat(".") + url.length();

    //System.out.println ("The SHA-256 hash of " + url + " is : " + sha256hex);
    return sha256hex;
  }
}
