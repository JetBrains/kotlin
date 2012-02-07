package stdlib.testall;

import junit.framework.TestSuite;
import test.dom.*;

/**
 */
public class DomTestAllTest {
  public static TestSuite suite() {
    return new TestSuite(DomBuilderTest.class, DomTest.class);
  }
}
