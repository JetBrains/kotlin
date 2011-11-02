package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class DeclarationStatementTest extends JetTestCaseBase {
  public void testSingleStringDeclaration() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("String s;"),
      "var s : String?"
    );
  }

  public void testSingleIntDeclaration() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("int s;"),
      "var s : Int"
    );
  }

  public void testMultiplyIntDeclaration() throws Exception {
    Assert.assertEquals(
      statementToKotlin("int k, l, m;"),
      "var k : Int\n" +
        "var l : Int\n" +
        "var m : Int"
    );
  }

  public void testSingleFinalStringDeclaration() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("final String s;"),
      "val s : String?"
    );
  }

  public void testSingleFinalIntDeclaration() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("final int s;"),
      "val s : Int"
    );
  }

  public void testMultiplyFinalIntDeclaration() throws Exception {
    Assert.assertEquals(
      statementToKotlin("final int k, l, m;"),
      "val k : Int\n" +
        "val l : Int\n" +
        "val m : Int"
    );
  }
}
