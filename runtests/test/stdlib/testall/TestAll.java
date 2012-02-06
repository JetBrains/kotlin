package stdlib.testall;

import junit.framework.TestSuite;
import test.collections.*;
import test.standard.*;

/**
 */
public class TestAll {
  public static TestSuite suite() {
    TestSuite suite = new TestSuite(GetOrElseTest.class, test.stdlib.issues.StdLibIssuesTest.class, StandardCollectionTest.class, CollectionTest.class, IoTest.class, ListTest.class, MapTest.class, SetTest.class, OldStdlibTest.class);
    suite.addTest(testDslExample.namespace.getSuite());
    return suite;
  }
}
