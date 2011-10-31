package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ParenthesizedExpressionTest extends JetTestCaseBase {
  public void testParenthesized() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("(1 + 2)"), "((1 + 2))");
  }

  public void testParenthesized2() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("(o.toString() + \"abc\")"), "((o.toString() + \"abc\"))");
  }
}