package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ReturnStatementTest extends JetTestCaseBase {
  public void testReturnLiteral() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("return true;"),
      "return true"
    );
  }

  public void testReturnString() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("return \"str\";"),
      "return \"str\""
    );
  }

  public void testReturnNumber() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("return 1;"),
      "return 1"
    );
  }

  public void testReturnChar() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("return 'c';"),
      "return 'c'"
    );
  }
}
