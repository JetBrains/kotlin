package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class TraitTest extends JetTestCaseBase {
  public void testEmptyInterface() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("interface A {}"),
      "trait A { }"
    );
  }

  public void testInterfaceWithMethodDeclaration() throws Exception {
    Assert.assertEquals(classToKotlin(
      "interface INode {" +
        "Tag getTag();" +
        "String toKotlin();" +
        "}"
    ),
      "trait INode {\n" +
        "public fun getTag() : Tag? \n" +
        "public fun toKotlin() : String? \n" +
        "}");
  }

  public void testInterfaceWithFields() throws Exception {
    Assert.assertEquals(classToKotlin(
      "interface INode {" +
        "String IN = \"in\";" +
        "String AT = \"@\";" +
        "String COMMA_WITH_SPACE = COMMA + SPACE;" +
        "}"
    ),
      "trait INode {\n" +
        "class object {\n" +
        "public val IN : String? = \"in\"\n" +
        "public val AT : String? = \"@\"\n" +
        "public val COMMA_WITH_SPACE : String? = (COMMA + SPACE)\n" +
        "}\n" +
        "}"
    );
  }

  public void testExtendsOneInterface() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("interface A extends I {}"),
      "trait A : I { }"
    );
  }

  public void testExtendsOneClassAndImplementsSeveralInterfaces() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("interface A extends I0, I1, I2 {}"),
      "trait A : I0, I1, I2 { }"
    );
  }

  public void testPublicInterface() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("public interface Test {}"), "public trait Test { }");
  }

  public void testProtectedInterface() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("protected interface Test {}"), "protected trait Test { }");
  }

  public void testPrivateInterface() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("private interface Test {}"), "private trait Test { }");
  }

  public void testInternalInterface() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("interface Test {}"), "trait Test { }");
  }

  public void testInterfaceWithStaticFields() throws Exception {
    Assert.assertEquals(classToKotlin(
      "public interface INode {" +
        "  public static final String IN = \"in\";" +
        "  public static final String AT = \"@\";" +
        "  public static final String COMMA_WITH_SPACE = COMMA + SPACE;" +
        "}"
    ),
      "public trait INode {\n" +
        "class object {\n" +
        "public val IN : String? = \"in\"\n" +
        "public val AT : String? = \"@\"\n" +
        "public val COMMA_WITH_SPACE : String? = (COMMA + SPACE)\n" +
        "}\n" +
        "}"
    );
  }
}