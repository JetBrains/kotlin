package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class NewClassExpressionTest extends JetTestCaseBase {
  public void testClassWithoutBody() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("new Foo();"), "Foo()");
  }

  public void testClassWithoutBody2() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("new myApp.Foo();"), "myApp.Foo()");
  }

  public void testClassWithParam() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("new Foo(param);"), "Foo(param)");
  }

  public void testClassWithParams() throws Exception {
    Assert.assertEquals(expressionToSingleLineKotlin("new Foo(param1, param2);"), "Foo(param1, param2)");
  }
}
