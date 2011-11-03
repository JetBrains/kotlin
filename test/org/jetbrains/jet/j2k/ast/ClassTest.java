package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class ClassTest extends JetTestCaseBase {
  public void testEmptyClass() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class A {}"),
      "class A { }"
    );
  }

  public void testInnerEmptyClass() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class A { inner final class B {} }"),
      "class A { class B { } }"
    );
  }

  public void testClassWithFields() throws Exception {
    Assert.assertEquals(classToKotlin(
      "final class T {" +
        "String a = \"abc\";" +
        "int b = 10;" +
        "}"),
      "class T {\n" +
        "var a : String? = \"abc\"\n" +
        "var b : Int = 10\n" +
        "}");
  }

  public void testClassWithMultiplyFields() throws Exception {
    Assert.assertEquals(classToKotlin(
      "final class T {" +
        "String a, b, c = \"abc\";" +
        "}"),
      "class T {\n" +
        "var a : String?\n" +
        "var b : String?\n" +
        "var c : String? = \"abc\"\n" +
        "}");
  }

  public void testClassWithEmptyMethods() throws Exception {
    Assert.assertEquals(classToKotlin(
      "final class T {" +
        "void main() {}" +
        "int i() {}" +
        "String s() {}" +
        "}"
    ),
      "class T {\n" +
        "fun main() : Unit {\n" +
        "}\n" +
        "fun i() : Int {\n" +
        "}\n" +
        "fun s() : String? {\n" +
        "}\n" +
        "}");
  }

  public void testGenericClass() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class Entry<K, V> {}"),
      "class Entry<K, V> { }"
    );
  }

  public void testSimpleInheritance() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class A extends Base {}"),
      "class A : Base { }"
    );
  }

  public void testExtendsOneClassAndImplementsOneInterface() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class A extends Base implements I {}"),
      "class A : Base, I { }"
    );
  }

  public void testExtendsOneClassAndImplementsSeveralInterfaces() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class A extends Base implements I0, I1, I2 {}"),
      "class A : Base, I0, I1, I2 { }"
    );
  }

  public void testClass() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("class Test {}"), "open class Test { }");
  }

  public void testFinalClass() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("final class Test {}"), "class Test { }");
  }

  public void testPublicClass() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("public class Test {}"), "public open class Test { }");
  }

  public void testProtectedClass() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("protected class Test {}"), "protected open class Test { }");
  }

  public void testPivateClass() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("private class Test {}"), "private open class Test { }");
  }

  public void testInternalClass() throws Exception {
    Assert.assertEquals(classToSingleLineKotlin("class Test {}"), "open class Test { }");
  }

  public void testOneStaticMethod() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class S { static boolean staticF() { return true; } }"),
      "class S { class object { fun staticF() : Boolean { return true } } }"
    );
  }

  public void testTwoStaticMethod() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class S { static boolean sB() { return true; } static int sI() { return 1; } }"),
      "class S { class object { fun sB() : Boolean { return true } fun sI() : Int { return 1 } } }"
    );
  }

  public void testOneStaticMethodOneNonStatic() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class S { boolean sB() { return true; } static int sI() { return 1; } }"),
      "class S { class object { fun sI() : Int { return 1 } } fun sB() : Boolean { return true } }"
    );
  }

  public void testOneStaticFieldOneNonStatic() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class S { boolean sB() { return true; } static int myI = 10; }"),
      "class S { class object { var myI : Int = 10 } fun sB() : Boolean { return true } }"
    );
  }
}
