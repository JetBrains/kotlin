package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class AssignmentExpressionTest extends JetTestCaseBase {
  public void testSimpleAssignment() throws Exception {
    Assert.assertEquals(statementToSingleLineKotlin("i = 1;"), "i = 1");
  }

  public void testShiftRight() throws Exception {
    Assert.assertEquals(expressionToKotlin("x >>= 2"), "x = (x shr 2)");
  }

  public void testShiftLeft() throws Exception {
    Assert.assertEquals(expressionToKotlin("x <<= 2"), "x = (x shl 2)");
  }

  public void testUnsignedRightShift() throws Exception {
    Assert.assertEquals(expressionToKotlin("x >>>= 2"), "x = x.cyclicShiftRight(2)");
  }

  public void testXor() throws Exception {
    Assert.assertEquals(expressionToKotlin("x ^= 2"), "x = (x xor 2)");
  }

  public void testAnd() throws Exception {
    Assert.assertEquals(expressionToKotlin("x &= 2"), "x = (x and 2)");
  }

  public void testOr() throws Exception {
    Assert.assertEquals(expressionToKotlin("x |= 2"), "x = (x or 2)");
  }

  public void testMultiplyAssign() throws Exception {
    Assert.assertEquals(expressionToKotlin("x *= 2"), "x *= 2");
  }

  public void testDivideAssign() throws Exception {
    Assert.assertEquals(expressionToKotlin("x /= 2"), "x /= 2");
  }

  public void testPlusAssign() throws Exception {
    Assert.assertEquals(expressionToKotlin("x += 2"), "x += 2");
  }

  public void testMinusAssign() throws Exception {
    Assert.assertEquals(expressionToKotlin("x -= 2"), "x -= 2");
  }

  public void testReminder() throws Exception {
    Assert.assertEquals(expressionToKotlin("x %= 2"), "x %= 2");
  }

  public void testAssignment() throws Exception {
    Assert.assertEquals(expressionToKotlin("x = 2"), "x = 2");
  }
}