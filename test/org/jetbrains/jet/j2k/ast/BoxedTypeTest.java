package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class BoxedTypeTest extends JetTestCaseBase {
  public void testObject() throws Exception {
    Assert.assertEquals(statementToKotlin("Object i = 10;"), "var i : Any? = 10");
  }

  public void testBoolean() throws Exception {
    Assert.assertEquals(statementToKotlin("Boolean i = 10;"), "var i : Boolean? = 10");
  }

  public void testByte() throws Exception {
    Assert.assertEquals(statementToKotlin("Byte i = 10;"), "var i : Byte? = 10");
  }

  public void testCharacter() throws Exception {
    Assert.assertEquals(statementToKotlin("Character i = 10;"), "var i : Char? = 10");
  }

  public void testDouble() throws Exception {
    Assert.assertEquals(statementToKotlin("Double i = 10;"), "var i : Double? = 10");
  }

  public void testFloat() throws Exception {
    Assert.assertEquals(statementToKotlin("Float i = 10;"), "var i : Float? = 10");
  }

  public void testInteger() throws Exception {
    Assert.assertEquals(statementToKotlin("Integer i = 10;"), "var i : Int? = 10");
  }

  public void testLong() throws Exception {
    Assert.assertEquals(statementToKotlin("Long i = 10;"), "var i : Long? = 10");
  }

  public void testShort() throws Exception {
    Assert.assertEquals(statementToKotlin("Short i = 10;"), "var i : Short? = 10");
  }

//  public void testNewInteger() throws Exception {
//    Assert.assertEquals(
//      statementToKotlin("Integer i = new Integer(10);"),
//      "var i : Int? = Integer(10).toInt()"
//    );
//  }
}
