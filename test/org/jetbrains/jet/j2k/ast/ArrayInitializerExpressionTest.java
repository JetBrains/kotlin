package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ArrayInitializerExpressionTest extends JetTestCaseBase {
  public void testOneDim() throws Exception {
    Assert.assertEquals(
      expressionToSingleLineKotlin("{1, 2, 3};"),
      "array(1, 2, 3)"
    );
  }

  public void testOneDimWithVariables() throws Exception {
    Assert.assertEquals(
      expressionToSingleLineKotlin("{a, b, c};"),
      "array(a, b, c)"
    );
  }

  public void testNewInt() throws Exception {
    Assert.assertEquals(
      expressionToSingleLineKotlin("new int[] {1, 2, 3};"),
      "array(1, 2, 3)"
    );
  }

  public void testTwoDim() throws Exception {
    Assert.assertEquals(
      expressionToSingleLineKotlin("{ {1, 2, 3}, {4, 5, 6}, {7, 8, 9} };"),
      "array(array(1, 2, 3), array(4, 5, 6), array(7, 8, 9))"
    );
  }

//  TODO
//  public void testTwoDimNewInt() throws Exception {
//    Assert.assertEquals(
//      statementToSingleLineKotlin("int[][] myArray = new int[5][];"),
//      "array(1, 2, 3)"
//    );
//  }
}
