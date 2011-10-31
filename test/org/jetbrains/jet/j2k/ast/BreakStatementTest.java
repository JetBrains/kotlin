package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class BreakStatementTest extends JetTestCaseBase {
  public void testBreakWithoutLabel() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("break;"),
      "break"
    );
  }

  public void testBreakWithLabel() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("break label;"),
      "break@label"
    );
  }
}
