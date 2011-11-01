package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class OutProjectionTypeTest extends JetTestCaseBase {
  public void testMethodParams() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("void pushAll(Collection<? extends E> src) {}"),
      "fun pushAll(src : Collection<out E?>?) : Unit { }"
    );
  }
}
