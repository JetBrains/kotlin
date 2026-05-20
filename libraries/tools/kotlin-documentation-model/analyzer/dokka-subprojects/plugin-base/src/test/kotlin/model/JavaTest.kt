/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.dokka.model.doc.Text
import utils.OnlyJavaSymbols
import utils.assertContains
import utils.assertNotNull
import utils.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaTest : BaseAbstractTest() {
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

    private inline fun <reified T : Documentable> Documentable?.cast(): T =
        (this as? T) ?: throw AssertionError("${T::class.simpleName} should not be null")

    private operator fun Documentable?.div(name: String): Documentable? =
        this?.children?.find { it.name == name }

    private infix fun Any?.equals(other: Any?) = assertEquals(other, this)
    private infix fun <T> Collection<T>?.counts(n: Int) =
        assertEquals(n, this.orEmpty().size, "Expected $n, got ${this.orEmpty().size}")

    @Test
    fun function() {
        testInline(
            """
            |/src/java/Test.java
            |package java;
            |class Test {
            |    /**
            |     * Summary for Function
            |     * @param name is String parameter
            |     * @param value is int parameter
            |     */
            |    public void fn(String name, int value) {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Test").cast<DClass>()) {
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
    }

    @Test
    fun allImplementedInterfacesInJava() {
        testInline(
            """
            |/src/java/Highest.java
            |package java;
            |interface Highest { }
            |/src/java/Lower.java
            |package java;
            |interface Lower extends Highest { }
            |/src/java/Extendable.java
            |package java;
            |class Extendable { }
            |/src/java/Tested.java
            |package java;
            |class Tested extends Extendable implements Lower { }
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Tested").cast<DClass>()) {
                    extra[ImplementedInterfaces]?.interfaces?.entries?.single()?.value?.map { it.dri.sureClassNames }
                        ?.sorted() equals listOf("Highest", "Lower").sorted()
                }
            }
        }
    }

    @Test
    fun allImplementedInterfacesWithGenericsInJava() {
        testInline(
            """
            |/src/java/Highest.java
            |package java;
            |interface Highest<H> { }
            |/src/java/Lower.java
            |package java;
            |interface Lower<L> extends Highest<L> { }
            |/src/java/Extendable.java
            |package java;
            |class Extendable { }
            |/src/java/Tested.java
            |package java;
            |class Tested<T> extends Extendable implements Lower<T> { }
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Tested").cast<DClass>()) {
                    val implementedInterfaces = extra[ImplementedInterfaces]?.interfaces?.entries?.single()?.value!!
                    implementedInterfaces.map { it.dri.sureClassNames }.sorted() equals listOf(
                        "Highest",
                        "Lower"
                    ).sorted()
                    for (implementedInterface in implementedInterfaces) {
                        assertEquals(
                            ((implementedInterface.projections.single() as Invariance<*>).inner as TypeParameter).name,
                            "T"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun multipleClassInheritanceWithInterface() {
        testInline(
            """
            |/src/java/Highest.java
            |package java;
            |interface Highest { }
            |/src/java/Lower.java
            |package java;
            |interface Lower extends Highest { }
            |/src/java/Extendable.java
            |package java;
            |class Extendable { }
            |/src/java/Tested.java
            |package java;
            |class Tested extends Extendable implements Lower { }
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Tested").cast<DClass>()) {
                    supertypes.entries.single().value.map { it.typeConstructor.dri.sureClassNames to it.kind }
                        .sortedBy { it.first } equals listOf(
                        "Extendable" to JavaClassKindTypes.CLASS,
                        "Lower" to JavaClassKindTypes.INTERFACE
                    )
                }
            }
        }
    }

    @Test
    fun interfaceWithGeneric() {
        testInline(
            """
            |/src/java/Bar.java
            |package java;
            |interface Bar<T> {}
            |/src/java/Foo.java
            |package java;
            |public class Foo implements Bar<String> {}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Foo").cast<DClass>()) {
                    val interfaceType = supertypes.values.flatten().single()
                    assertEquals(interfaceType.kind, JavaClassKindTypes.INTERFACE)
                    assertEquals(interfaceType.typeConstructor.dri.classNames, "Bar")
                    val generic =
                        (interfaceType.typeConstructor.projections.single() as Invariance<*>).inner as GenericTypeConstructor
                    assertEquals(generic.dri.classNames, "String")
                }
            }
        }
    }

    @Test
    fun superClass() {
        testInline(
            """
            |/src/java/Foo.java
            |package java;
            |public class Foo extends Exception implements Cloneable {}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Foo").cast<DClass>()) {
                    val sups = listOf("Exception", "Cloneable")
                    assertTrue(
                        sups.all { s -> supertypes.values.flatten().any { it.typeConstructor.dri.classNames == s } })
                    "Foo must extend ${sups.joinToString(", ")}"
                }
            }
        }
    }

    @Test
    fun superclassWithGeneric() {
        testInline(
            """
            |/src/java/Bar.java
            |package java;
            |class Bar<T> {}
            |/src/java/Foo.java
            |package java;
            |public class Foo extends Bar<String> {}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Foo").cast<DClass>()) {
                    val superclassType = supertypes.values.flatten().single()
                    assertEquals(superclassType.kind, JavaClassKindTypes.CLASS)
                    assertEquals(superclassType.typeConstructor.dri.classNames, "Bar")
                    val generic =
                        (superclassType.typeConstructor.projections.single() as Invariance<*>).inner as GenericTypeConstructor
                    assertEquals(generic.dri.classNames, "String")
                }
            }
        }
    }

    @Test
    fun arrayType() {
        testInline(
            """
            |/src/java/Test.java
            |package java;
            |class Test {
            |    public String[] arrayToString(int[] data) {
            |      return null;
            |    }
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Test").cast<DClass>()) {
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
    }

    @Test
    fun typeParameter() {
        testInline(
            """
            |/src/java/Foo.java
            |package java;
            |class Foo<T extends Comparable<T>> {
            |     public <E> E foo();
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Foo").cast<DClass>()) {
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
    }

    @Test
    fun typeParameterIntoDifferentClasses2596() {
        testInline(
            """
            |/src/java/GenericDocument.java
            |package java;
            |class GenericDocument { }
            |/src/java/DocumentClassFactory.java
            |package java;
            |public interface DocumentClassFactory<T> {
            |    String getSchemaName();
            |    GenericDocument toGenericDocument(T document);
            |    T fromGenericDocument(GenericDocument genericDoc);
            |}
            |/src/java/DocumentClassFactoryRegistry.java
            |package java;
            |public final class DocumentClassFactoryRegistry {
            |    public <T> DocumentClassFactory<T> getOrCreateFactory(T documentClass) {
            |        return null;
            |    }
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "DocumentClassFactory").cast<DInterface>()) {
                    generics counts 1
                    generics[0].dri.classNames equals "DocumentClassFactory"
                }
                with((module / "java" / "DocumentClassFactoryRegistry").cast<DClass>()) {
                    functions.forEach {
                        (it.type as GenericTypeConstructor).dri.classNames equals "DocumentClassFactory"
                        (((it.type as GenericTypeConstructor).projections[0] as Invariance<*>).inner as TypeParameter).dri.classNames equals "DocumentClassFactoryRegistry"
                    }
                }
            }
        }
    }

    @Test
    fun constructors() {
        testInline(
            """
            |/src/java/Test.java
            |package java;
            |class Test {
            |  public Test() {}
            |
            |  public Test(String s) {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Test").cast<DClass>()) {
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
    }

    @Test
    fun innerClass() {
        testInline(
            """
            |/src/java/InnerClass.java
            |package java;
            |class InnerClass {
            |    public class D {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "InnerClass").cast<DClass>()) {
                    children counts 2 // default constructor and inner class
                    with((this / "D").cast<DClass>()) {
                        name equals "D"
                        children counts 1 // default constructor
                    }
                }
            }
        }
    }

    @OnlyJavaSymbols("Java symbols have `KotlinOnlyModifiers.VarArg`")
    @Test
    fun varargs() {
        testInline(
            """
            |/src/java/Foo.java
            |package java;
            |class Foo {
            |     public void bar(String... x);
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Foo").cast<DClass>()) {
                    name equals "Foo"
                    children counts 2 // default constructor and function

                    with((this / "bar").cast<DFunction>()) {
                        name equals "bar"
                        with(parameters.firstOrNull().assertNotNull("parameter")) {
                            name equals "x"
                            type.name equals "Array"
                            assertEquals(
                                setOf(ExtraModifiers.KotlinOnlyModifiers.VarArg),
                                extra[AdditionalModifiers]?.content?.values?.first(),
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun fields() {
        testInline(
            """
            |/src/java/Test.java
            |package java;
            |class Test {
            |  public int i;
            |  public static final String s;
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Test").cast<DClass>()) {
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
    }

    @Test
    fun staticMethod() {
        testInline(
            """
            |/src/java/C.java
            |package java;
            |class C {
            |  public static void foo() {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "C" / "foo").cast<DFunction>()) {
                    with(extra[AdditionalModifiers]!!.content.entries.single().value.assertNotNull("AdditionalModifiers")) {
                        this counts 1
                        first() equals ExtraModifiers.JavaOnlyModifiers.Static
                    }
                }
            }
        }
    }

    @Test
    fun throwsList() {
        testInline(
            """
            |/src/java/C.java
            |package java;
            |class C {
            |  public void foo() throws java.io.IOException, ArithmeticException {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "C" / "foo").cast<DFunction>()) {
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
    }

    @Test
    fun annotatedAnnotation() {
        testInline(
            """
            |/src/java/Attribute.java
            |package java;
            |import java.lang.annotation.*;
            |
            |@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
            |public @interface Attribute {
            |  String value() default "";
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Attribute").cast<DAnnotation>()) {
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
    }

    @Test
    fun javaLangObject() {
        testInline(
            """
            |/src/java/Test.java
            |package java;
            |class Test {
            |  public Object fn() { return null; }
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Test" / "fn").cast<DFunction>()) {
                    assertTrue(type is JavaObject)
                }
            }
        }
    }

    @Test
    fun enumValues() {
        testInline(
            """
            |/src/java/E.java
            |package java;
            |enum E {
            |  Foo
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "E").cast<DEnum>()) {
                    name equals "E"
                    entries counts 1
                    with((this / "Foo").cast<DEnumEntry>()) {
                        name equals "Foo"
                    }
                }
            }
        }
    }

    @Test
    fun inheritorLinks() {
        testInline(
            """
            |/src/java/InheritorLinks.java
            |package java;
            |public class InheritorLinks {
            |  public static class Foo {}
            |
            |  public static class Bar extends Foo {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "InheritorLinks").cast<DClass>()) {
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

    @Test
    fun `retention should work with static import`() {
        testInline(
            """
            |/src/java/JsonClass.java
            |package java;
            |import java.lang.annotation.Retention;
            |import java.lang.annotation.RetentionPolicy;
            |import static java.lang.annotation.RetentionPolicy.RUNTIME;
            |
            |@Retention(RUNTIME)
            |public @interface JsonClass {
            |};
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "JsonClass").cast<DAnnotation>()) {
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
    }

    @Test
    fun variances() {
        testInline(
            """
            |/src/java/Foo.java
            |package java;
            |public class Foo {
            |    public void superBound(java.util.List<? super String> param) {}
            |    public void extendsBound(java.util.List<? extends String> param) {}
            |    public void unbounded(java.util.List<?> param) {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Foo").cast<DClass>()) {
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
                                assertTrue(variance is Star)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should have a link to a package in see doctag`() {
        testInline(
            """
            |/src/java/Foo.java
            |package java;
            |/**
            | * @see java
            | */
            |public class Foo {
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Foo").cast<DClass>()) {
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

    @Test
    @OnlyJavaSymbols("Java PSI incorrectly omits generating a synthetic property without a backing field")
    fun `synthetic properties should have a Java type`() {
        testInline(
            """
            |/src/java/Foo.java
            |package java;
            |public class Foo {
            | String getA() {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Foo" / "a").cast< DProperty>()) {
                    val expectedDRI = DRI(
                        packageName = "java.lang", classNames = "String",
                        target = PointingToDeclaration,
                    )
                    assertEquals(expectedDRI, this.type.driOrNull)
                }
            }
        }
    }
}
