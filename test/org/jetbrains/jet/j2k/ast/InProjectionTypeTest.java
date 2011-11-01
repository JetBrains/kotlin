package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class InProjectionTypeTest extends JetTestCaseBase {
  public void testMethodParams() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("void popAll(Collection<? super E> dst) {}"),
      "fun popAll(dst : Collection<in E?>?) : Unit { }"
    );
  }
}
