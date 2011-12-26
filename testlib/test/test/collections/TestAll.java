package test.collections;

import junit.framework.TestSuite;

/**
 */
public class TestAll {
  public static TestSuite suite() {
    TestSuite suite = new TestSuite(CollectionTest.class, IoTest.class, ListTest.class, MapTest.class, SetTest.class);
    suite.addTest(testDslExample.namespace.getSuite());
    return suite;
  }
}