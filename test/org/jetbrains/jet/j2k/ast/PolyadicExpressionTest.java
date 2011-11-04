package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class PolyadicExpressionTest extends JetTestCaseBase {
  public void testMultiply() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 * 2 * 3"), "(1 * 2 * 3)");
  }

  public void testDivide() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 / 2 / 3"), "(1 / 2 / 3)");
  }

  public void testRemainder() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 % 2 % 3"), "(1 % 2 % 3)");
  }

  public void testPlus() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 + 2 + 3"), "(1 + 2 + 3)");
  }

  public void testMinus() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 - 2 - 3"), "(1 - 2 - 3)");
  }
}
