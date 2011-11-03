package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class EnumTest extends JetTestCaseBase {
  public void testEmptyEnum() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("enum A {}"),
      "enum A { }"
    );
  }

  public void testTypeSafeEnum() throws Exception {
    Assert.assertEquals(
      classToKotlin("enum Coin { PENNY, NICKEL, DIME, QUARTER; }"),
      "enum Coin {\n" +
        "PENNY\n" +
        "NICKEL\n" +
        "DIME\n" +
        "QUARTER\n" +
        "}"
    );
  }

  public void testOverrideToString() throws Exception {
    Assert.assertEquals(
      classToKotlin(
        "enum Color {" +
          " WHITE, BLACK, RED, YELLOW, BLUE;" +
          "@Override String toString() {" +
          "  return \"COLOR\";" +
          "}" +
          "}"),
      "enum Color {\n" +
        "WHITE\n" +
        "BLACK\n" +
        "RED\n" +
        "YELLOW\n" +
        "BLUE\n" +
        "fun toString() : String? {\n" +
        "return \"COLOR\"\n" +
        "}\n" +
        "}"
    );
  }

  public void testFields() throws Exception {
    Assert.assertEquals(
      classToKotlin(
        "enum Color {\n" +
          " WHITE(21), BLACK(22), RED(23), YELLOW(24), BLUE(25);\n" +
          "\n" +
          " private int code;\n" +
          "\n" +
          " private Color(int c) {\n" + // TODO: private constructor, WTF?
          "   code = c;\n" +
          " }\n" +
          "\n" +
          " public int getCode() {\n" +
          "   return code;\n" +
          " }"),
      "enum Color(c : Int) {\n" +
        "WHITE(21)\n" +
        "BLACK(22)\n" +
        "RED(23)\n" +
        "YELLOW(24)\n" +
        "BLUE(25)\n" +
        "var code : Int\n" +
        "public fun getCode() : Int {\n" +
        "return code\n" +
        "}\n" +
        "}"
    );
  }

  public void testEnumImplementsOneInterface() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("enum A implements I {}"),
      "enum A : I { }"
    );
  }

  public void testEnumImplementsSeveralInterfaces() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("enum A implements I0, I1, I2 {}"),
      "enum A : I0, I1, I2 { }"
    );
  }

//  public void testTwoConstructors() throws Exception {
//    Assert.assertEquals(
//      classToKotlin(
//        "enum MultEnum {\n" +
//          "    GREMLIN(\"UTILITY\"),\n" +
//          "    MORT(30);\n" +
//          "  \n" +
//          "    MultEnum(String s) {\n" +
//          "    }\n" +
//          "  \n" +
//          "    MultEnum(int dmg) {\n" +
//          "    }"),
//      "" // TODO: will fail
//    );
//  }
//
//  public void testInterfaceImplementation() throws Exception {
//    Assert.assertEquals(
//      classToKotlin(
//        "enum Color implements Runnable {\n" +
//          " WHITE, BLACK, RED, YELLOW, BLUE;\n" +
//          "\n" +
//          " public void run() {\n" +
//          "   System.out.println(\"name()=\" + name() +\n" +
//          "       \", toString()=\" + toString());\n" +
//          " }\n" +
//          "}"),
//      ""
//    );
//  }
}
