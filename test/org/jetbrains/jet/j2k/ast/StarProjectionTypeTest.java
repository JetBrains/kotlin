package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class StarProjectionTypeTest extends JetTestCaseBase {
  public void testMethodParams() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("void wtf(Collection<?> w) {}"),
      "fun wtf(w : Collection<*>?) : Unit { }"
    );
  }
}
