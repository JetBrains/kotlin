package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class FieldTest extends JetTestCaseBase {
  public void testVarWithoutInit() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("Foo f;"),
      "var f : Foo?"
    );
  }

  public void testVarWithInit() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("Foo f = new Foo(1, 2);"),
      "var f : Foo? = Foo(1, 2)"
    );
  }

  public void testValWithInit() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("final Foo f = new Foo(1, 2);"),
      "val f : Foo? = Foo(1, 2)"
    );
  }
}
