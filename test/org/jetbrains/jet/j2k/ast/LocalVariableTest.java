package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class LocalVariableTest extends JetTestCaseBase {
  public void testObject() throws Exception {
    Assert.assertEquals(
      statementToSingleLineKotlin("int i;"),
      "var i : Int"
    );
  }
}
