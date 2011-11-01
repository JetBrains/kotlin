package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class TypeParametersTest extends JetTestCaseBase {
  public void testGenericParam() throws Exception {
    Assert.assertEquals(
      statementToKotlin("List<T> l;"),
      "var l : List<T?>?"
    );
  }

  public void testManyGenericParams() throws Exception {
    Assert.assertEquals(
      statementToKotlin("List<T, K, M> l;"),
      "var l : List<T?, K?, M?>?"
    );
  }

}
