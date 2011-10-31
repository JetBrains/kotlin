package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ArrayAccessExpressionTest extends JetTestCaseBase {
  public void testIntIndex() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("myArray[10]"), "myArray[10]");
  }

  public void testVariableIndex() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("myArray[i]"), "myArray[i]");
  }

  public void testExpressionIndex() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("myArray[myLibrary.calculateIndex(100)]"), "myArray[myLibrary.calculateIndex(100)]");
  }
}
