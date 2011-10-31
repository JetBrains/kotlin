package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class AssertStatementTest extends JetTestCaseBase {
  public void testOnlyCondition() throws Exception {
    Assert.assertEquals(
      statementToKotlin("assert boolMethod();"),
      "assert {boolMethod()}"
    );
  }

  public void testOnlyConditionWithBraces() throws Exception {
    Assert.assertEquals(
      statementToKotlin("assert(boolMethod());"),
      "assert {(boolMethod())}"
    );
  }

  public void testWithStringDetail() throws Exception {
    Assert.assertEquals(
      statementToKotlin("assert true : \"string details\";"),
      "assert(\"string details\") {true}"
    );
  }
}
