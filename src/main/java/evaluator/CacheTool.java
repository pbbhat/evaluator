package evaluator;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Parameters;

import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@Command(
    description = "Provides functionality to manually writing cached HTML",
    name = "cache-tool",
    mixinStandardHelpOptions = true,
    version = "0.1")
public class CacheTool implements Callable<Void> {
  @Parameters(index = "0", description = "Url for the cache entry")
  private String url;

  @Parameters(index = "1", description = "Path to file with HTML content to add to the cache")
  private File htmlFile;

  @Parameters(index = "2", description = "Folder for HTML cache")
  private String htmlCacheFolderName;

  private HTMLCache htmlCache = null;

  public static void main(String[] args) {
    CommandLine.call(new CacheTool(), args);
  }

  @Override
  public Void call() throws Exception {
    htmlCache = new HTMLCache(htmlCacheFolderName);
    htmlCache.insertDocument(url, Files.readAllBytes(htmlFile.toPath()));
    return null;
  }
}
