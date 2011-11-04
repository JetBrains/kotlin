package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class FileTest extends JetTestCaseBase {
  public void testPackageWithImports() throws Exception {
    Assert.assertEquals(
      fileToKotlin(
        "package test;" +
          "import ast;" +
          "import ast2;"),
      "namespace test {" + "\n" +
        "import ast" + "\n" +
        "import ast2" + "\n" +
        "}"
    );
  }

  public void testPackageWithMixedImports() throws Exception {
    Assert.assertEquals(
      fileToKotlin(
        "package test;" +
          "import static ast;" +
          "import ast2;"),
      "namespace test {" + "\n" +
        "import ast" + "\n" +
        "import ast2" + "\n" +
        "}"
    );
  }

  public void testPackageWithStaticImports() throws Exception {
    Assert.assertEquals(
      fileToKotlin(
        "package test;" +
          "import static ast;" +
          "import static ast2;"),
      "namespace test {" + "\n" +
        "import ast" + "\n" +
        "import ast2" + "\n" +
        "}"
    );
  }

  public void testPackageWithClass() throws Exception {
    Assert.assertEquals(
      toSingleLine(fileToKotlin("package test; final class C {}")),
      "namespace test { class C { } }"
    );
  }

  public void testPackageWithOpenClass() throws Exception {
    Assert.assertEquals(
      toSingleLine(fileToKotlin("package test; class C {}")),
      "namespace test { open class C { } }"
    );
  }

  public void testPackageWithClasses() throws Exception {
    Assert.assertEquals(
      toSingleLine(fileToKotlin("final class A {} final class B {}")),
      "namespace { class A { } class B { } }");
  }
}
