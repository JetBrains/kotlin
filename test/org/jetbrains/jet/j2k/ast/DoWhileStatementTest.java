package org.jetbrains.jet.j2k.ast;

import org.jetbrains.jet.j2k.JetTestCaseBase;
import org.junit.Assert;

/**
 * @author ignatov
 */
public class DoWhileStatementTest extends JetTestCaseBase {
  public void testWhileWithEmptyBlock() throws Exception {
    Assert.assertEquals(
      statementToKotlin("do {} while (true)"),
      "do\n" +
        "{\n" +
        "}\n" +
        "while (true)"
    );
  }

  public void testWhileWithBlock() throws Exception {
    Assert.assertEquals(
      statementToKotlin("do {int i = 1; i = i + 1;} while (a > b)"),
      "do\n" +
        "{\n" +
        "var i : Int = 1\n" +
        "i = (i + 1)\n" +
        "}\n" +
        "while ((a > b))"
    );
  }

  public void testWhileWithReturn() throws Exception {
    Assert.assertEquals(
      statementToKotlin("do return 1; while (true)"),
      "do\nreturn 1\nwhile (true)"
    );
  }

  public void testWhileWithExpression() throws Exception {
    Assert.assertEquals(
      statementToKotlin("do i = i + 1; while (true)"),
      "do\ni = (i + 1)\nwhile (true)"
    );
  }
}
