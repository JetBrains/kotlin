package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class BinaryExpressionTest extends JetTestCaseBase {
  public void testMultiply() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 * 2"), "(1 * 2)");
  }

  public void testDivide() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 / 2"), "(1 / 2)");
  }

  public void testRemainder() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 % 2"), "(1 % 2)");
  }

  public void testPlus() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 + 2"), "(1 + 2)");
  }

  public void testMinus() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 - 2"), "(1 - 2)");
  }

  public void testLessThan() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 < 2"), "(1 < 2)");
  }

  public void testGreaterThan() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 > 2"), "(1 > 2)");
  }

  public void testLessThanEqual() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 <= 2"), "(1 <= 2)");
  }

  public void testGreaterThanEqual() throws Exception {
    Assert.assertEquals(expressionToKotlin("1 >= 2"), "(1 >= 2)");
  }

  public void testConditionalAnd() throws Exception {
    Assert.assertEquals(expressionToKotlin("true && false"), "(true && false)");
  }

  public void testConditionalOr() throws Exception {
    Assert.assertEquals(expressionToKotlin("true || false"), "(true || false)");
  }

  public void testShiftRight() throws Exception {
    Assert.assertEquals(expressionToKotlin("x >> 2"), "(x shr 2)");
  }

  public void testShiftLeft() throws Exception {
    Assert.assertEquals(expressionToKotlin("x << 2"), "(x shl 2)");
  }

  public void testUnsignedRightShift() throws Exception {
    Assert.assertEquals(expressionToKotlin("x >>> 2"), "x.cyclicShiftRight(2)");
  }

  public void testXor() throws Exception {
    Assert.assertEquals(expressionToKotlin("x ^ 2"), "(x xor 2)");
  }

  public void testAnd() throws Exception {
    Assert.assertEquals(expressionToKotlin("x & 2"), "(x and 2)");
  }

  public void testOr() throws Exception {
    Assert.assertEquals(expressionToKotlin("x | 2"), "(x or 2)");
  }

//  public void testNot() throws Exception { // TODO: move to prefix
//    Assert.assertEquals(expressionToKotlin("~x"), "inv(x)");
//  }
}
