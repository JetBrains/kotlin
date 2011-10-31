package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ForTest extends JetTestCaseBase {
  public void testCommonCaseForTest() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (init(); condition(); update()) body();"),
      "{\n" +
        "init()\n" +
        "while (condition())\n" +
        "{\n" +
        "body()\n" +
        "{\n" +
        "update()\n" +
        "}\n" +
        "}\n" +
        "}"
    );
  }

  public void testForWithEmptyBlock() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (int i = 0; i < 0; j++, i++) {}"),
      "{\n" +
        "var i : Int = 0\n" +
        "while ((i < 0))\n" +
        "{\n" +
        "{\n" +
        "}\n" +
        "{\n" +
        "(j++)\n" +
        "(i++)\n" +
        "}\n" +
        "}\n" +
        "}"
    );
  }

  public void testForWithBlock() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (int i = 0; i < 0; i++) {int i = 1; i++;}"),
      "{\n" +
        "var i : Int = 0\n" +
        "while ((i < 0))\n" +
        "{\n" +
        "{\n" +
        "var i : Int = 1\n" +
        "(i++)\n" +
        "}\n" +
        "{\n" +
        "(i++)\n" +
        "}\n" +
        "}\n" +
        "}"
    );
  }

  public void testForWithBlockAndDoubleUpdate() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (int i = 0; i < 0; j++, i++) {int i = 1; i++;}"),
      "{\n" +
        "var i : Int = 0\n" +
        "while ((i < 0))\n" +
        "{\n" +
        "{\n" +
        "var i : Int = 1\n" +
        "(i++)\n" +
        "}\n" +
        "{\n" +
        "(j++)\n" +
        "(i++)\n" +
        "}\n" +
        "}\n" +
        "}"
    );
  }

  public void testForWithReturn() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (int i = 0; i < 0; j++, i++) return i;"),
      "{\n" +
        "var i : Int = 0\n" +
        "while ((i < 0))\n" +
        "{\n" +
        "return i\n" +
        "{\n" +
        "(j++)\n" +
        "(i++)\n" +
        "}\n" +
        "}\n" +
        "}"
    );
  }

  public void testForWithExpression() throws Exception {
    Assert.assertEquals(
      statementToKotlin("for (int i = 0; i < 0; i++) t++;"),
      "{\n" +
        "var i : Int = 0\n" +
        "while ((i < 0))\n" +
        "{\n" +
        "(t++)\n" +
        "{\n" +
        "(i++)\n" +
        "}\n" +
        "}\n" +
        "}"
    );
  }
}
