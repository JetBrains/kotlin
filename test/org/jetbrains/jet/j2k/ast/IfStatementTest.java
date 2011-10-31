package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class IfStatementTest extends JetTestCaseBase {
  public void testIfStatementWithOneLineBlocks() throws Exception {
    Assert.assertEquals(
      statementToKotlin("if (true) return 1; else return 0;"),
      "if (true)\n" +
        "return 1\n" +
        "else\n" +
        "return 0"
    );
  }

  public void testIfStatementWithMultilineBlocks() throws Exception {
    Assert.assertEquals(
      statementToKotlin("if (1 > 0) {int n = 1; return n;} else {return 0;}"),
      "if ((1 > 0))\n" +
        "{\n" +
        "var n : Int = 1\n" +
        "return n\n" +
        "}\n" +
        "else\n" +
        "{\n" +
        "return 0\n" +
        "}"
    );
  }

  public void testIfStatementWithoutElse() throws Exception {
    Assert.assertEquals(
      statementToKotlin("if (1 > 0) {int n = 1; return n;}"),
      "if ((1 > 0))\n" +
        "{\n" +
        "var n : Int = 1\n" +
        "return n\n" +
        "}"
    );
  }

  public void testIfStatementWithEmptyBlocks() throws Exception {
    Assert.assertEquals(
      statementToKotlin("if (1 > 0) {} else {}"),
      "if ((1 > 0))\n" +
        "{\n" +
        "}\n" +
        "else\n" +
        "{\n" +
        "}"
    );
  }
}
