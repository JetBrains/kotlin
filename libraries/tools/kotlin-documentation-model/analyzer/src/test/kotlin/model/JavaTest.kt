/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.analyzer.ids.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.kotlin.testFederation.MustRunAlways
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInAnalysisApi
import utils.assertContains
import utils.assertNotNull
import utils.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@MustRunOnChangesInAnalysisApi
class JavaTest : BaseAbstractTest() {
    internal val configuration = dokkaConfiguration {
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

    /** The classifier declaration ID of a (possibly nullable) class type. */
    private fun Bound.classifierDeclarationId(): KdElementId {
        val bound = if (this is Nullable) inner else this
        return (bound as? GenericTypeConstructor)?.declarationId
            ?: throw AssertionError("Expected a class type, got $this")
    }

    /** The declaration ID of [entry] in the [enumClass] Java enum, without resolving the symbol. */
    private fun javaEnumEntryId(enumClass: String, entry: String): KdElementId =
        enumEntryDeclarationId(ClassId.topLevel(FqName(enumClass)), entry)

    @Test
    @MustRunAlways
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
                    extra[ImplementedInterfaces]?.interfaces?.entries?.single()?.value
                        ?.mapNotNull { declarationIdToClassNames(it.declarationId) }
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
                    implementedInterfaces.mapNotNull { declarationIdToClassNames(it.declarationId) }.sorted() equals listOf(
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
                    supertypes.entries.single().value.map { it.typeConstructor.declarationId to it.kind }
                        .sortedBy { it.first.value } equals listOf(
                        KdElementId("java/Extendable") to JavaClassKindTypes.CLASS,
                        KdElementId("java/Lower") to JavaClassKindTypes.INTERFACE
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
                    assertEquals("Bar", declarationIdToClassNames(interfaceType.typeConstructor.declarationId))
                    val generic =
                        (interfaceType.typeConstructor.projections.single() as Invariance<*>).inner as GenericTypeConstructor
                    assertEquals("String", declarationIdToClassNames(generic.declarationId))
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
                        sups.all { s -> supertypes.values.flatten().any { declarationIdToClassNames(it.typeConstructor.declarationId) == s } },
                        "Foo must extend ${sups.joinToString(", ")}"
                    )
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
                    assertEquals("Bar", declarationIdToClassNames(superclassType.typeConstructor.declarationId))
                    val generic =
                        (superclassType.typeConstructor.projections.single() as Invariance<*>).inner as GenericTypeConstructor
                    assertEquals("String", declarationIdToClassNames(generic.declarationId))
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
                            type.name equals "IntArray"
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
                    // A type parameter is not a declaration: its ID hangs off the owning class's ID
                    ownerDeclarationIdOf(generics[0].declarationId) equals KdElementId("java/Foo")
                    (functions[0].type as? TypeParameter)?.containerDeclarationId?.run {
                        // the type parameter `E` is declared by the `foo` method
                        declarationIdToPackageName(this) equals "java"
                        callableNameOf(this) equals "foo"
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
                    ownerDeclarationIdOf(generics[0].declarationId) equals KdElementId("java/DocumentClassFactory")
                }
                with((module / "java" / "DocumentClassFactoryRegistry").cast<DClass>()) {
                    functions.forEach {
                        declarationIdToClassNames((it.type as GenericTypeConstructor).declarationId) equals "DocumentClassFactory"
                        val typeParameter = (it.type.projections[0] as Invariance<*>).inner as TypeParameter
                        declarationIdToClassNames(typeParameter.containerDeclarationId) equals "DocumentClassFactoryRegistry"
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
                            type.name equals "String"
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
    fun `static method is marked as companion-block member`() {
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
                    @OptIn(ExperimentalDokkaApi::class)
                    extra[IsCompanion].assertNotNull("CompanionBlockMember on Java static method")
                }
            }
        }
    }

    @Test
    fun `instance method is not marked as companion-block member`() {
        testInline(
            """
            |/src/java/C.java
            |package java;
            |class C {
            |  public void instanceFoo() {}
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "C" / "instanceFoo").cast<DFunction>()) {
                    @OptIn(ExperimentalDokkaApi::class) assertEquals(null, extra[IsCompanion])
                }
            }
        }
    }

    @Test
    fun `static field is marked as companion-block member`() {
        testInline(
            """
            |/src/java/C.java
            |package java;
            |class C {
            |  public static final String s = "x";
            |  public int i;
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "C" / "s").cast<DProperty>()) {
                    @OptIn(ExperimentalDokkaApi::class)
                    extra[IsCompanion].assertNotNull("CompanionBlockMember on Java static field")
                }
                with((module / "java" / "C" / "i").cast<DProperty>()) {
                    @OptIn(ExperimentalDokkaApi::class) assertEquals(null, extra[IsCompanion])
                }
            }
        }
    }

    @Test
    fun `enum synthetic values, valueOf are marked as companion-block members`() {
        testInline(
            """
            |/src/java/E.java
            |package java;
            |enum E {
            |  A, B
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "E").cast<DEnum>()) {
                    val values = functions.firstOrNull { it.name == "values" }
                        .assertNotNull("synthetic 'values' function")
                    val valueOf = functions.firstOrNull { it.name == "valueOf" }
                        .assertNotNull("synthetic 'valueOf' function")

                    @OptIn(ExperimentalDokkaApi::class) values.extra[IsCompanion].assertNotNull("CompanionBlockMember on enum 'values'")
                    @OptIn(ExperimentalDokkaApi::class) valueOf.extra[IsCompanion].assertNotNull("CompanionBlockMember on enum 'valueOf'")
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
                        declarationIdToPackageName(first()) equals "java.io"
                        declarationIdToClassNames(first()) equals "IOException"
                        declarationIdToPackageName(get(1)) equals "java.lang"
                        declarationIdToClassNames(get(1)) equals "ArithmeticException"
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
                            classLikeId equals KdElementId("java.lang.annotation/Target")
                            (params["value"].assertNotNull("value") as ArrayValue).value equals listOf(
                                EnumValue("ElementType.FIELD", javaEnumEntryId("java.lang.annotation.ElementType", "FIELD")),
                                EnumValue("ElementType.TYPE", javaEnumEntryId("java.lang.annotation.ElementType", "TYPE")),
                                EnumValue("ElementType.METHOD", javaEnumEntryId("java.lang.annotation.ElementType", "METHOD"))
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
                    // Java `Object` is mapped to `kotlin.Any` by the Analysis API
                    assertEquals(KdElementId("kotlin/Any"), type.classifierDeclarationId())
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
                    val barId = (this / "Bar").assertNotNull("Bar").declarationId
                    with((this / "Foo").cast<DClass>()) {
                        with(extra[InheritorsInfo].assertNotNull("InheritorsInfo")) {
                            with(value.values.flatten().distinct()) {
                                this counts 1
                                first() equals barId
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

                    val expectedAnnotationId = KdElementId("java.lang.annotation/Retention")
                    val expectedParams = "value" to EnumValue(
                        "RUNTIME",
                        javaEnumEntryId("java.lang.annotation.RetentionPolicy", "RUNTIME")
                    )

                    assertEquals(expectedAnnotationId, annotation?.classLikeId)
                    assertEquals(expectedParams.first, annotation?.params?.entries?.first()?.key)
                    assertEquals(expectedParams.second, annotation?.params?.entries?.first()?.value)
                }
            }
        }
    }

    @Test
    fun `source enum constant in annotation param gets canonical enum-entry id`() {
        testInline(
            """
            |/src/java/Level.java
            |package java;
            |public enum Level {
            |    LOW, HIGH
            |}
            |
            |/src/java/Marker.java
            |package java;
            |public @interface Marker {
            |    Level value();
            |}
            |
            |/src/java/Annotated.java
            |package java;
            |@Marker(Level.HIGH)
            |public class Annotated {
            |}
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                with((module / "java" / "Annotated").cast<DClass>()) {
                    val annotation = extra[Annotations]?.directAnnotations?.entries
                        ?.firstOrNull()?.value // First source set
                        ?.firstOrNull()

                    assertEquals(
                        "Marker" to EnumValue("Level.HIGH", javaEnumEntryId("java.Level", "HIGH")),
                        annotation?.let { declarationIdToClassNames(it.classLikeId) to it.params.entries.firstOrNull()?.value }
                    )
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
                                assertEquals("String", declarationIdToClassNames((bound as GenericTypeConstructor).declarationId))
                            }

                            "extendsBound" -> {
                                assertTrue(variance is Covariance<*>)
                                val bound = variance.inner
                                assertEquals("String", declarationIdToClassNames((bound as GenericTypeConstructor).declarationId))
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
                    assertEquals(KdElementId("java"), (doc.dfs { it is See } as See).address)
                }
            }
        }
    }

    @Test
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
                with((module / "java" / "Foo" / "a").cast<DProperty>()) {
                    assertEquals(KdElementId("kotlin/String"), type.classifierDeclarationId())
                }
            }
        }
    }

    @Test
    fun `java members inherited into a kotlin class should have kotlin types`() {
        testInline(
            """
            |/src/test/Parent.java
            |package test;
            |public class Parent {
            |    public String s = null;
            |    public int i = 0;
            |    public Integer integer = null;
            |    public Integer method() { return null; }
            |}
            |
            |/src/test/Child.kt
            |package test
            |class Child : Parent()
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                val child = module / "test" / "Child"

                // Java types are mapped to Kotlin types by the Analysis API
                (child / "s").cast<DProperty>().type.classifierDeclarationId() equals KdElementId("kotlin/String")
                (child / "i").cast<DProperty>().type.classifierDeclarationId() equals KdElementId("kotlin/Int")
                (child / "integer").cast<DProperty>().type.classifierDeclarationId() equals KdElementId("kotlin/Int")
                (child / "method").cast<DFunction>().type.classifierDeclarationId() equals KdElementId("kotlin/Int")
            }
        }
    }
}
