package model

import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Text
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull
import utils.name

class JavaTest : AbstractModelTest("/src/main/kotlin/java/Test.java", "java") {

    @Test
    fun function() {
        inlineModelTest(
            """
            |class Test {
            |    /**
            |     * Summary for Function
            |     * @param name is String parameter
            |     * @param value is int parameter
            |     */
            |    public void fn(String name, int value) {}
            |}
            """
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                name equals "Test"
                children counts 1
                with((this / "fn").cast<DFunction>()) {
                    name equals "fn"
                    val params = parameters.map { it.documentation.values.first().children.first() as Param }
                    params.mapNotNull { it.firstChildOfTypeOrNull<Text>()?.body } equals listOf("is String parameter", "is int parameter")
                }
            }
        }
    }

    @Test fun allImplementedInterfacesInJava() {
        inlineModelTest(
            """
            |interface Highest { }
            |interface Lower extends Highest { }
            |class Extendable { }
            |class Tested extends Extendable implements Lower { }
        """){
            with((this / "java" / "Tested").cast<DClass>()){
                extra[ImplementedInterfaces]?.interfaces?.entries?.single()?.value?.map { it.sureClassNames }?.sorted() equals listOf("Highest", "Lower").sorted()
            }
        }
    }

    @Test fun multipleClassInheritanceWithInterface() {
        inlineModelTest(
            """
            |interface Highest { }
            |interface Lower extends Highest { }
            |class Extendable { }
            |class Tested extends Extendable implements Lower { }
        """){
            with((this / "java" / "Tested").cast<DClass>()) {
                supertypes.entries.single().value.map { it.dri.sureClassNames to it.kind }.sortedBy { it.first } equals listOf("Extendable" to JavaClassKindTypes.CLASS, "Lower" to JavaClassKindTypes.INTERFACE)
            }
        }
    }

    @Test // todo
    fun memberWithModifiers() {
        inlineModelTest(
            """
            |class Test {
            |    /**
            |     * Summary for Function
            |     * @param name is String parameter
            |     * @param value is int parameter
            |     */
            |    public void fn(String name, int value) {}
            |}
            """
        ) {
            with((this / "java" / "Test" / "fn").cast<DFunction>()) {
                this
            }
        }
    }

    @Test
    fun superClass() {
        inlineModelTest(
            """
            |public class Foo extends Exception implements Cloneable {}
            """
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                val sups = listOf("Exception", "Cloneable")
                assertTrue(
                    sups.all { s -> supertypes.values.flatten().any { it.dri.classNames == s } })
                "Foo must extend ${sups.joinToString(", ")}"
            }
        }
    }

    @Test
    fun arrayType() {
        inlineModelTest(
            """
            |class Test {
            |    public String[] arrayToString(int[] data) {
            |      return null;
            |    }
            |}
            """
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                name equals "Test"
                children counts 1

                with((this / "arrayToString").cast<DFunction>()) {
                    name equals "arrayToString"
                    type.name equals "Array"
                    with(parameters.firstOrNull().assertNotNull("parameters")) {
                        name equals "data"
                        type.name equals "Array"
                    }
                }
            }
        }
    }

    @Test
    fun typeParameter() {
        inlineModelTest(
            """
            |class Foo<T extends Comparable<T>> {
            |     public <E> E foo();
            |}
            """
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                generics counts 1
            }
        }
    }

    @Test
    fun constructors() {
        inlineModelTest(
            """
            |class Test {
            |  public Test() {}
            |
            |  public Test(String s) {}
            |}
            """
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                name equals "Test"

                constructors counts 2
                constructors.find { it.parameters.isNullOrEmpty() }.assertNotNull("Test()")

                with(constructors.find { it.parameters.isNotEmpty() }.assertNotNull("Test(String)")) {
                    parameters.firstOrNull()?.type?.name equals "String"
                }
            }
        }
    }

    @Test
    fun innerClass() {
        inlineModelTest(
            """
            |class InnerClass {
            |    public class D {}
            |}
            """
        ) {
            with((this / "java" / "InnerClass").cast<DClass>()) {
                children counts 1
                with((this / "D").cast<DClass>()) {
                    name equals "D"
                    children counts 0
                }
            }
        }
    }

    @Test
    fun varargs() {
        inlineModelTest(
            """
            |class Foo {
            |     public void bar(String... x);
            |}
            """
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                name equals "Foo"
                children counts 1

                with((this / "bar").cast<DFunction>()) {
                    name equals "bar"
                    with(parameters.firstOrNull().assertNotNull("parameter")) {
                        name equals "x"
                        type.name equals "Array"
                    }
                }
            }
        }
    }

    @Test // todo
    fun fields() {
        inlineModelTest(
            """
            |class Test {
            |  public int i;
            |  public static final String s;
            |}
            """
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                children counts 2

                with((this / "i").cast<DProperty>()) {
                    getter equals null
                    setter equals null
                }

                with((this / "s").cast<DProperty>()) {
                    getter equals null
                    setter equals null
                }
            }
        }
    }

    @Test
    fun staticMethod() {
        inlineModelTest(
            """
            |class C {
            |  public static void foo() {}
            |}
            """
        ) {
            with((this / "java" / "C" / "foo").cast<DFunction>()) {
                with(extra[AdditionalModifiers]!!.content.entries.single().value.assertNotNull("AdditionalModifiers")) {
                    this counts 1
                    first() equals ExtraModifiers.JavaOnlyModifiers.Static
                }
            }
        }
    }

    @Test
    fun annotatedAnnotation() {
        inlineModelTest(
            """
            |import java.lang.annotation.*;
            |
            |@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
            |public @interface Attribute {
            |  String value() default "";
            |}
            """
        ) {
            with((this / "java" / "Attribute").cast<DAnnotation>()) {
                with(extra[Annotations]!!.content.entries.single().value.assertNotNull("Annotations")) {
                    with(single()) {
                        dri.classNames equals "Target"
                        (params["value"].assertNotNull("value") as ArrayValue).value equals listOf(
                            EnumValue("ElementType.FIELD", DRI("java.lang.annotation", "ElementType")),
                            EnumValue("ElementType.TYPE", DRI("java.lang.annotation", "ElementType")),
                            EnumValue("ElementType.METHOD", DRI("java.lang.annotation", "ElementType"))
                        )
                    }
                }
            }
        }
    }

    @Test
    fun javaLangObject() {
        inlineModelTest(
            """
            |class Test {
            |  public Object fn() { return null; }
            |}
            """
        ) {
            with((this / "java" / "Test" / "fn").cast<DFunction>()) {
                assertTrue(type is JavaObject)
            }
        }
    }

    @Test
    fun enumValues() {
        inlineModelTest(
            """
            |enum E {
            |  Foo
            |}
            """
        ) {
            with((this / "java" / "E").cast<DEnum>()) {
                name equals "E"
                entries counts 1

                with((this / "Foo").cast<DEnumEntry>()) {
                    name equals "Foo"
                }
            }
        }
    }

    @Test
    fun inheritorLinks() {
        inlineModelTest(
            """
            |public class InheritorLinks {
            |  public static class Foo {}
            |
            |  public static class Bar extends Foo {}
            |}
            """
        ) {
            with((this / "java" / "InheritorLinks").cast<DClass>()) {
                val dri = (this / "Bar").assertNotNull("Foo dri").dri
                with((this / "Foo").cast<DClass>()) {
                    with(extra[InheritorsInfo].assertNotNull("InheritorsInfo")) {
                        with(value.values.flatten().distinct()) {
                            this counts 1
                            first() equals dri
                        }
                    }
                }
            }
        }
    }
}
