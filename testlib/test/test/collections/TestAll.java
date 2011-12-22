package test.collections;

import junit.framework.TestSuite;

/**
 */
public class TestAll {
  public static TestSuite suite() {
    return new TestSuite(CollectionTest.class, IoTest.class, ListTest.class, MapTest.class, SetTest.class);
  }
}