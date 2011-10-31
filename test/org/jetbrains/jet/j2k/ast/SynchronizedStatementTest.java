package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class SynchronizedStatementTest extends JetTestCaseBase {
  public void testSingleLineExample() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("synchronized (s) { doSomething(s); }"),
      "synchronized (s) { doSomething(s) }"
    );
  }
}
