package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class LabelStatementTest extends JetTestCaseBase {
  public void testComplicatedExampleFromJavaTutorial() throws Exception {
    Assert.assertEquals(
      statementToKotlin(
        "    test:" +
          "        for (int i = 0; i <= max; i++) {" +
          "            int n = substring.length();" +
          "            int j = i;" +
          "            int k = 0;" +
          "            while (n-- != 0) {" +
          "                if (searchMe.charAt(j++) != substring.charAt(k++)) {" +
          "                    continue test;" +
          "                }" +
          "            }    " +
          "            foundIt = true;" +
          "            break test;" +
          "        }" +
          "        System.out.println(foundIt ? \"Found it\" : \"Didn't find it\");" +
          "    }" +
          "}"),
      "@test {\n" +
        "var i : Int = 0\n" +
        "while ((i <= max))\n" +
        "{\n" +
        "{\n" +
        "var n : Int = substring.length()\n" +
        "var j : Int = i\n" +
        "var k : Int = 0\n" +
        "while (((n--) != 0))\n" +
        "{\n" +
        "if ((searchMe.charAt((j++)) != substring.charAt((k++))))\n" +
        "{\n" +
        "continue@test\n" +
        "}\n" +
        "}\n" +
        "foundIt = true\n" +
        "break@test\n" +
        "}\n" +
        "{\n" +
        "(i++)\n" +
        "}\n" +
        "}\n" +
        "}\n" +
        "System.out.println((if (foundIt)\n" +
        "\"Found it\"\n" +
        "else\n" +
        "\"Didn't find it\"))"
    );
  }
}
