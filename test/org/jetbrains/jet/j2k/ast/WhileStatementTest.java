package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class WhileStatementTest extends JetTestCaseBase {
  public void testWhileWithEmptyBlock() throws Exception {
    Assert.assertEquals(
      statementToKotlin("while (true) {}"),
      "while (true)\n{\n}"
    );
  }

  public void testWhileWithBlock() throws Exception {
    Assert.assertEquals(
      statementToKotlin("while (a > b) {int i = 1; i = i + 1;}"),
      "while ((a > b))\n" +
        "{\n" +
        "var i : Int = 1\n" +
        "i = (i + 1)\n" +
        "}"
    );
  }

  public void testWhileWithReturn() throws Exception {
    Assert.assertEquals(
      statementToKotlin("while (true) return 1;"),
      "while (true)\nreturn 1"
    );
  }

  public void testWhileWithExpression() throws Exception {
    Assert.assertEquals(
      statementToKotlin("while (true) i = i + 1;"),
      "while (true)\ni = (i + 1)"
    );
  }
}
