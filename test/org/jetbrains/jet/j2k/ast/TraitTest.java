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
        "fun getTag() : Tag? \n" +
        "fun toKotlin() : String? \n" +
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
        "var IN : String? = \"in\"\n" +
        "var AT : String? = \"@\"\n" +
        "var COMMA_WITH_SPACE : String? = (COMMA + SPACE)\n" +
        "}"
    );
  }
}