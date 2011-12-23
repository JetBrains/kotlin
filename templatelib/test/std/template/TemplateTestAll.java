package std.template;

import std.template.html.*;

import junit.framework.TestSuite;

/**
 */
public class TemplateTestAll {
  public static TestSuite suite() {
    return new TestSuite(TemplateCoreTest.class, TemplateHtmlTest.class);
  }
}
