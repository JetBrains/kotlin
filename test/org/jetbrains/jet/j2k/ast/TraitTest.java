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
        "val IN : String? = \"in\"\n" +
        "val AT : String? = \"@\"\n" +
        "val COMMA_WITH_SPACE : String? = (COMMA + SPACE)\n" +
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
}
