package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class VarArgTest extends JetTestCaseBase {
  public void testEllipsisTypeSingleParams() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("void pushAll(Object... objs) {}"),
      "fun pushAll(vararg objs : Object?) : Unit { }"
    );
  }

  public void testEllipsisTypeSeveralParams() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("String format(String pattern, Object... arguments);"),
      "fun format(pattern : String?, vararg arguments : Object?) : String?"
    );
  }
}
