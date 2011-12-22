package test.collections;

import junit.framework.TestSuite;

/**
 */
public class TestAll {
  public static TestSuite suite() {
    return new TestSuite(CollectionTest.class, SetTest.class, ListTest.class, IoTest.class, MapTest.class);
  }
}