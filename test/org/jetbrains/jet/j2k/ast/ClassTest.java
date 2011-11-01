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
}
