package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class MethodCallExpressionTest extends JetTestCaseBase {
  public void testSimpleCall() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("method(param1, param2);"), "method(param1, param2)"
    );
  }

  public void testEmptyCall() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("methodCall();"), "methodCall()"
    );
  }

  public void testCallWithKeywords() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("when(open, trait);"), "`when`(`open`, `trait`)"
    );
  }
}
