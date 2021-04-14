package model

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Text
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull
import utils.name
import kotlin.test.assertEquals
import  org.jetbrains.dokka.links.Callable as DRICallable

class JavaTest : AbstractModelTest("/src/main/kotlin/java/Test.java", "java") {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = Platform.jvm.toString()
                classpath += jvmStdlibPath!!
                includeNonPublic = true
            }
        }
    }

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
            """, configuration = configuration
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                name equals "Test"
                children counts 1
                with((this / "fn").cast<DFunction>()) {
                    name equals "fn"
                    val params = parameters.map { it.documentation.values.first().children.first() as Param }
                    params.mapNotNull { it.firstMemberOfType<Text>()?.body } equals listOf("is String parameter", "is int parameter")
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
        """, configuration = configuration){
            with((this / "java" / "Tested").cast<DClass>()){
                extra[ImplementedInterfaces]?.interfaces?.entries?.single()?.value?.map { it.dri.sureClassNames }?.sorted() equals listOf("Highest", "Lower").sorted()
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
        """, configuration = configuration){
            with((this / "java" / "Tested").cast<DClass>()) {
                supertypes.entries.single().value.map { it.typeConstructor.dri.sureClassNames to it.kind }.sortedBy { it.first } equals listOf("Extendable" to JavaClassKindTypes.CLASS, "Lower" to JavaClassKindTypes.INTERFACE)
            }
        }
    }

    @Test
    fun superClass() {
        inlineModelTest(
            """
            |public class Foo extends Exception implements Cloneable {}
            """, configuration = configuration
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                val sups = listOf("Exception", "Cloneable")
                assertTrue(
                    sups.all { s -> supertypes.values.flatten().any { it.typeConstructor.dri.classNames == s } })
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
            """, configuration = configuration
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
            """, configuration = configuration
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
            """, configuration = configuration
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                name equals "Test"

                constructors counts 2
                constructors.forEach { it.name equals "Test" }
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
            """, configuration = configuration
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
            """, configuration = configuration
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

    @Test
    fun fields() {
        inlineModelTest(
            """
            |class Test {
            |  public int i;
            |  public static final String s;
            |}
            """, configuration = configuration
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
            """, configuration = configuration
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
            """, configuration = configuration
        ) {
            with((this / "java" / "Attribute").cast<DAnnotation>()) {
                with(extra[Annotations]!!.directAnnotations.entries.single().value.assertNotNull("Annotations")) {
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
            """, configuration = configuration
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
            """, configuration = configuration
        ) {
            with((this / "java" / "E").cast<DEnum>()) {
                name equals "E"
                entries counts 1
                functions.sortedBy { it.name }.filter { it.name == "valueOf" || it.name == "values" }.map { it.dri } equals listOf(
                    DRI("java", "E", DRICallable("valueOf", null, listOf(JavaClassReference("java.lang.String"))), PointingToDeclaration),
                    DRI("java", "E", DRICallable("values", null, emptyList()), PointingToDeclaration),
                )

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
            """, configuration = configuration
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

    @Test
    fun `retention should work with static import`() {
        inlineModelTest(
            """
            |import java.lang.annotation.Retention;
            |import java.lang.annotation.RetentionPolicy;
            |import static java.lang.annotation.RetentionPolicy.RUNTIME;
            |
            |@Retention(RUNTIME)
            |public @interface JsonClass {
            |};
            """, configuration = configuration
        ) {
            with((this / "java" / "JsonClass").cast<DAnnotation>()) {
                val annotation = extra[Annotations]?.directAnnotations?.entries
                    ?.firstOrNull()?.value //First sourceset
                    ?.firstOrNull()

                val expectedDri = DRI("java.lang.annotation", "Retention", null, PointingToDeclaration)
                val expectedParams = "value" to EnumValue(
                    "RUNTIME",
                    DRI(
                        "java.lang.annotation",
                        "RetentionPolicy",
                        DRICallable("RUNTIME", null, emptyList()),
                        PointingToDeclaration
                    )
                )

                assertEquals(expectedDri, annotation?.dri)
                assertEquals(expectedParams.first, annotation?.params?.entries?.first()?.key)
                assertEquals(expectedParams.second, annotation?.params?.entries?.first()?.value)
            }
        }
    }

}
