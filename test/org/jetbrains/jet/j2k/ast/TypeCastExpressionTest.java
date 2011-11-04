package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class TypeCastExpressionTest extends JetTestCaseBase {
  public void testStringCast() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("(String)t"),
      "(t as String?)"
    );
  }

  public void testIntCast() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("(int)t"),
      "(t as Int)"
    );
  }

  public void testFooCast() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("(Foo)t"),
      "(t as Foo?)"
    );
  }

  public void testPrimitiveType() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("(int)100.00;"),
      "(100.00 as Int)"
    );
  }

  public void testSimpleGenericCast() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("(List<Expression>)list"),
      "(list as List<Expression?>?)"
    );
  }

  public void testExtendsWildcardCast() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("(List<? extends String>)list"),
      "(list as List<out String?>?)"
    );
  }

  public void testSuperWildcardCast() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("(List<? super String>)list"),
      "(list as List<in String?>?)"
    );
  }

  public void testWildcardCast() throws Exception {
    Assert.assertEquals(
      expressionToKotlin("(List<?>)list"),
      "(list as List<*>?)"
    );
  }
}