package evaluator;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

public class NoOpEntityResolver implements EntityResolver {

  @Override
  public InputSource resolveEntity(String publicId, String systemId)
      throws SAXException, IOException {
    return null;
  }
}
