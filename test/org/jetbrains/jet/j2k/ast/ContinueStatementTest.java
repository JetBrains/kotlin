package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ContinueStatementTest extends JetTestCaseBase {
  public void testContinueWithoutLabel() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("continue;"),
      "continue"
    );
  }

  public void testContinueWithLabel() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("continue label;"),
      "continue@label"
    );
  }
}
