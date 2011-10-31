package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ForeachStatementTest extends JetTestCaseBase {
  public void testEnhancedForWithEmptyBlock() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (Node n : list) {}"),
      "for (n : Node? in list)\n{\n" + "}"
    );
  }

  public void testEnhancedForWithBlock() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (Node n : list) {int i = 1; i++;}"),
      "for (n : Node? in list)\n" +
        "{\n" +
        "var i : Int = 1\n" +
        "(i++)\n" +
        "}"
    );
  }

  public void testEnhancedForWithReturn() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (Node n : list) return n;"),
      "for (n : Node? in list)\nreturn n"
    );
  }

  public void testEnhancedForWithExpression() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (Node n : list) i++;"),
      "for (n : Node? in list)\n(i++)"
    );
  }
}
