package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class IsOperatorTest extends JetTestCaseBase {
  public void testSimpleReference() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("a instanceof String"),
      "(a is String?)"
    );
  }

  public void testComplicatedExpression() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("c.getType().getName() instanceof String"),
      "(c.getType().getName() is String?)"
    );
  }
}
