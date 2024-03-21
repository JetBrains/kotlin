/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.dokka.model.doc.Text
import utils.AbstractModelTest
import utils.assertContains
import utils.assertNotNull
import utils.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaTest : AbstractModelTest("/src/main/kotlin/java/Test.java", "java") {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = Platform.jvm.toString()
                classpath += jvmStdlibPath!!
                documentedVisibilities = setOf(
                    DokkaConfiguration.Visibility.PUBLIC,
                    DokkaConfiguration.Visibility.PRIVATE,
                    DokkaConfiguration.Visibility.PROTECTED,
                    DokkaConfiguration.Visibility.PACKAGE,
                )
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
                children counts 2 // default constructor and function
                with((this / "fn").cast<DFunction>()) {
                    name equals "fn"
                    val params = parameters.map { it.documentation.values.first().children.first() as Param }
                    params.map { it.firstMemberOfType<Text>().body } equals listOf(
                        "is String parameter",
                        "is int parameter"
                    )
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
                children counts 2 // default constructor and function

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
                generics[0].dri.classNames equals "Foo"
                (functions[0].type as? TypeParameter)?.dri?.run {
                    packageName equals "java"
                    name equals "Foo"
                    callable?.name equals "foo"
                }
            }
        }
    }

    @Test
    fun typeParameterIntoDifferentClasses2596() {
        inlineModelTest(
            """
            |class GenericDocument { }
            |public interface DocumentClassFactory<T> {
            |    String getSchemaName();
            |    GenericDocument toGenericDocument(T document);
            |    T fromGenericDocument(GenericDocument genericDoc);
            |}
            |
            |public final class DocumentClassFactoryRegistry {
            |    public <T> DocumentClassFactory<T> getOrCreateFactory(T documentClass) {
            |        return null;
            |    }
            |}
            """, configuration = configuration
        ) {
            with((this / "java" / "DocumentClassFactory").cast<DInterface>()) {
                generics counts 1
                generics[0].dri.classNames equals "DocumentClassFactory"
            }
            with((this / "java" / "DocumentClassFactoryRegistry").cast<DClass>()) {
                functions.forEach {
                    (it.type as GenericTypeConstructor).dri.classNames equals "DocumentClassFactory"
                    ((it.type as GenericTypeConstructor).projections[0] as TypeParameter).dri.classNames equals "DocumentClassFactoryRegistry"
                }
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
                constructors.find { it.parameters.isEmpty() }.assertNotNull("Test()")

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
                children counts 2 // default constructor and inner class
                with((this / "D").cast<DClass>()) {
                    name equals "D"
                    children counts 1 // default constructor
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
                children counts 2 // default constructor and function

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
                children counts 3 // default constructor + 2 props

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
    fun throwsList() {
        inlineModelTest(
            """
            |class C {
            |  public void foo() throws java.io.IOException, ArithmeticException {}
            |}
            """, configuration = configuration
        ) {
            with((this / "java" / "C" / "foo").cast<DFunction>()) {
                with(extra[CheckedExceptions]?.exceptions?.entries?.single()?.value.assertNotNull("CheckedExceptions")) {
                    this counts 2
                    first().packageName equals "java.io"
                    first().classNames equals "IOException"
                    get(1).packageName equals "java.lang"
                    get(1).classNames equals "ArithmeticException"
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
                        "RetentionPolicy.RUNTIME",
                        null,
                        PointingToDeclaration,
                        DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
                    )
                )

                assertEquals(expectedDri, annotation?.dri)
                assertEquals(expectedParams.first, annotation?.params?.entries?.first()?.key)
                assertEquals(expectedParams.second, annotation?.params?.entries?.first()?.value)
            }
        }
    }

    @Test
    fun variances() {
        inlineModelTest(
            """
            |public class Foo {
            |    public void superBound(java.util.List<? super String> param) {}
            |    public void extendsBound(java.util.List<? extends String> param) {}
            |    public void unbounded(java.util.List<?> param) {}
            |}
            """, configuration = configuration
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                val functionNames = functions.map { it.name }
                assertContains(functionNames, "superBound")
                assertContains(functionNames, "extendsBound")
                assertContains(functionNames, "unbounded")

                for (function in functions) {
                    val param = function.parameters.single()
                    val type = param.type as GenericTypeConstructor
                    val variance = type.projections.single()

                    when (function.name) {
                        "superBound" -> {
                            assertTrue(variance is Contravariance<*>)
                            val bound = variance.inner
                            assertEquals((bound as GenericTypeConstructor).dri.classNames, "String")
                        }
                        "extendsBound" -> {
                            assertTrue(variance is Covariance<*>)
                            val bound = variance.inner
                            assertEquals((bound as GenericTypeConstructor).dri.classNames, "String")
                        }
                        "unbounded" -> {
                            assertTrue(variance is Covariance<*>)
                            val bound = variance.inner
                            assertTrue(bound is JavaObject)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should have a link to a package in see doctag`() {
        inlineModelTest(
            """
            |/**
            | * @see java
            | */
            |public class Foo {
            |}
            """, configuration = configuration
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                val doc = this.documentation.values.single()
                val expectedDRI = DRI(
                    packageName = "java", classNames = null,
                    target = PointingToDeclaration,
                )
                assertEquals(expectedDRI, (doc.dfs { it is See } as See).address)
            }
        }
    }
}
