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

  public void testPrivateField() throws Exception {
    Assert.assertEquals(methodToSingleLineKotlin("private Foo f;"), "private var f : Foo?");
  }

  public void testProtectedField() throws Exception {
    Assert.assertEquals(methodToSingleLineKotlin("protected Foo f;"), "protected var f : Foo?");
  }

  public void testPublicField() throws Exception {
    Assert.assertEquals(methodToSingleLineKotlin("public Foo f;"), "public var f : Foo?");
  }

  public void testInternalField() throws Exception {
    Assert.assertEquals(methodToSingleLineKotlin("Foo f;"), "var f : Foo?");
  }
}
