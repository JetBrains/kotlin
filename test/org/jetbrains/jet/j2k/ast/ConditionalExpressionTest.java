package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ConditionalExpressionTest extends JetTestCaseBase {
  public void testSimpleConditionalExpression() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("a.isEmpty() ? 0 : 1"),
      "(if (a.isEmpty())\n" +
        "0\n" +
        "else\n" +
        "1)"
    );
  }
}