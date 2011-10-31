package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ThrowStatementTest extends JetTestCaseBase {
  public void testSimpleThrowStatement() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("throw exception;"), "throw exception");
  }
}
