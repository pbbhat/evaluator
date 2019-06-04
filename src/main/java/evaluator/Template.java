package evaluator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import net.sf.saxon.s9api.XPathCompiler;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Template {
  @JsonProperty
  String pattern;

  Predicate<String> urlMatch;

  @JsonProperty
  String domain;

  @JsonProperty
  String name;

  @JsonProperty
  List<Rule> rules;

  public void validateAndInitialize(XPathCompiler xPathCompiler) {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(pattern),
        "Pattern must be non-empty!");
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(domain),
        "Domain must be non-empty!");
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(name),
        "Name must be non-empty!");
    Preconditions.checkNotNull(rules, "At least one rule must be specified");
    Preconditions.checkArgument(rules.size() > 0, "At least one rule must be specified");
    urlMatch = Pattern.compile(pattern).asPredicate(); // Make sure we can compile the pattern
    Preconditions.checkArgument(pattern.contains(domain), "The pattern should contain the domain");
    for (Rule rule : rules) {
      rule.validateAndInitialize(xPathCompiler);
    }
  }
}
