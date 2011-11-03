package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class PrefixOperatorTest extends JetTestCaseBase {
  public void testIncrement() throws Exception {
    Assert.assertEquals(statementToKotlin("++i;"), "(++i)");
  }

  public void testDecrement() throws Exception {
    Assert.assertEquals(statementToKotlin("--i;"), "(--i)");
  }
}
