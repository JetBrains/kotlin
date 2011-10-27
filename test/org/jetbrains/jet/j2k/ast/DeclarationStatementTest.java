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

}
