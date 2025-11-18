/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.model.*
import utils.AbstractModelTest
import utils.OnlySymbols
import utils.assertNotNull
import utils.name
import utils.text
import kotlin.test.Test
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.links.Nullable
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.links.TypeParam
import org.jetbrains.dokka.links.TypeReference

@OptIn(ExperimentalDokkaApi::class)
class PropertyTest : AbstractModelTest("/src/main/kotlin/property/Test.kt", "property") {

    @Test
    fun valueProperty() {
        inlineModelTest(
            """
            |val property = "test""""
        ) {
            with((this / "property" / "property").cast<DProperty>()) {
                name equals "property"
                children counts 0
                with(getter.assertNotNull("Getter")) {
                    type.name equals "String"
                }
                type.name equals "String"
            }
        }
    }

    @Test
    fun variableProperty() {
        inlineModelTest(
            """
            |var property = "test"
            """
        ) {
            with((this / "property" / "property").cast<DProperty>()) {
                name equals "property"
                children counts 0
                setter.assertNotNull("Setter")
                with(getter.assertNotNull("Getter")) {
                    type.name equals "String"
                }
                type.name equals "String"
            }
        }
    }

    @Test
    fun valuePropertyWithGetter() {
        inlineModelTest(
            """
            |val property: String
            |    get() = "test"
            """
        ) {
            with((this / "property" / "property").cast<DProperty>()) {
                name equals "property"
                children counts 0
                with(getter.assertNotNull("Getter")) {
                    type.name equals "String"
                }
                type.name equals "String"
            }
        }
    }

    @Test
    fun variablePropertyWithAccessors() {
        inlineModelTest(
            """
            |var property: String
            |    get() = "test"
            |    set(value) {}
            """
        ) {
            with((this / "property" / "property").cast<DProperty>()) {
                name equals "property"
                children counts 0
                setter.assertNotNull("Setter")
                with(getter.assertNotNull("Getter")) {
                    type.name equals "String"
                }
                visibility.values allEquals KotlinVisibility.Public
            }
        }
    }

    @Test
    fun propertyWithReceiver() {
        inlineModelTest(
            """
            |val String.property: Int
            |    get() = size() * 2
            """
        ) {
            with((this / "property" / "property").cast<DProperty>()) {
                name equals "property"
                children counts 0
                with(receiver.assertNotNull("property receiver")) {
                    name equals null
                    type.name equals "String"
                }
                with(getter.assertNotNull("Getter")) {
                    type.name equals "Int"
                }
                visibility.values allEquals KotlinVisibility.Public
            }
        }
    }

    @Test
    fun propertyOverride() {
        inlineModelTest(
            """
            |open class Foo() {
            |    open val property: Int get() = 0
            |}
            |class Bar(): Foo() {
            |    override val property: Int get() = 1
            |}
            """
        ) {
            with((this / "property").cast<DPackage>()) {
                with((this / "Foo" / "property").cast<DProperty>()) {
                    dri.classNames equals "Foo"
                    name equals "property"
                    children counts 0
                    with(getter.assertNotNull("Getter")) {
                        type.name equals "Int"
                    }
                }
                with((this / "Bar" / "property").cast<DProperty>()) {
                    dri.classNames equals "Bar"
                    name equals "property"
                    children counts 0
                    with(getter.assertNotNull("Getter")) {
                        type.name equals "Int"
                    }
                }
            }
        }
    }

    @Test
    fun propertyInherited() {
        inlineModelTest(
            """
            |open class Foo() {
            |    open val property: Int get() = 0
            |}
            |class Bar(): Foo()
            """
        ) {
            with((this / "property").cast<DPackage>()) {
                with((this / "Bar" / "property").cast<DProperty>()) {
                    dri.classNames equals "Foo"
                    name equals "property"
                    children counts 0
                    with(getter.assertNotNull("Getter")) {
                        type.name equals "Int"
                        extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                            classNames equals "Foo"
                            callable equals null
                        }
                    }
                    extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                        classNames equals "Foo"
                        callable equals null
                    }
                }
            }
        }
    }

    @Test
    fun `accessors of inherited property should have correct extra`() {
        inlineModelTest(
            """
            |open class Foo() {
            |    var property = 0
            |}
            |class Bar(): Foo()
            """
        ) {
            with((this / "property").cast<DPackage>()) {
                with((this / "Bar" / "property").cast<DProperty>()) {
                    with(getter.assertNotNull("Getter")) {
                        type.name equals "Int"
                        extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                            classNames equals "Foo"
                            callable equals null
                        }
                    }
                    with(setter.assertNotNull("Setter")) {
                        type.name equals "Unit"
                        extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                            classNames equals "Foo"
                            callable equals null
                        }
                    }
                    extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                        classNames equals "Foo"
                        callable equals null
                    }
                }
            }
        }
    }

    @Test
    fun sinceKotlin() {
        inlineModelTest(
            """
                |/**
                | * Quite useful [String]
                | */
                |@SinceKotlin("1.1")
                |val prop: String = "1.1 rulezz"
                """
        ) {
            with((this / "property" / "prop").cast<DProperty>()) {
                with(extra[Annotations]!!.directAnnotations.entries.single().value.assertNotNull("Annotations")) {
                    this counts 1
                    with(first()) {
                        dri.classNames equals "SinceKotlin"
                        params.entries counts 1
                        (params["version"].assertNotNull("version") as StringValue).value equals "1.1"
                    }
                }
            }
        }
    }

    @Test
    fun annotatedProperty() {
        inlineModelTest(
            """
                |@Strictfp var property = "test"
                """,
            configuration = dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/")
                        classpath = listOfNotNull(jvmStdlibPath)
                    }
                }
            }
        ) {
            with((this / "property" / "property").cast<DProperty>()) {
                with(extra[Annotations]!!.directAnnotations.entries.single().value.assertNotNull("Annotations")) {
                    this counts 1
                    with(first()) {
                        dri.classNames equals "Strictfp"
                        params.entries counts 0
                    }
                }
            }
        }
    }

    @Test
    fun genericTopLevelExtensionProperty() {
        inlineModelTest(
            """ | val <T : Number> List<T>.sampleProperty: T
                |   get() { TODO() }
            """.trimIndent()
        ) {
            with((this / "property" / "sampleProperty").cast<DProperty>()) {
                name equals "sampleProperty"
                with(receiver.assertNotNull("Property receiver")) {
                    type.name equals "List"
                }
                with(getter.assertNotNull("Getter")) {
                    type.name equals "T"
                }
                setter equals null
                generics counts 1
                generics.forEach {
                    it.name equals "T"
                    it.bounds.first().name equals "Number"
                }
                visibility.values allEquals KotlinVisibility.Public
            }
        }
    }

    @Test
    fun genericExtensionPropertyInClass() {
        inlineModelTest(
            """ | package test
                | class XD<T> {
                |   var List<T>.sampleProperty: T
                |       get() { TODO() }
                |       set(value) { TODO() }
                | }
            """.trimIndent()
        ) {
            with((this / "property" / "XD" / "sampleProperty").cast<DProperty>()) {
                name equals "sampleProperty"
                children counts 0
                with(receiver.assertNotNull("Property receiver")) {
                    type.name equals "List"
                }
                with(getter.assertNotNull("Getter")) {
                    type.name equals "T"
                    val receiver = TypeConstructor(
                        "kotlin.collections.List",
                        listOf(
                            TypeParam(
                                name = "T",
                                bounds = listOf(Nullable(TypeConstructor("kotlin.Any", emptyList())))
                            )
                        )
                    )

                    // In K1 a name of an accessor is `<get-sampleProperty>`, in K2 - `getSampleProperty`
                    this.dri.packageName equals "property"
                    this.dri.classNames equals "XD"
                    this.dri.callable?.receiver equals receiver
                    this.dri.callable?.params equals emptyList<TypeReference>()
                }
                with(setter.assertNotNull("Setter")) {
                    type.name equals "Unit"

                    val receiver = TypeConstructor(
                        "kotlin.collections.List",
                        listOf(
                            TypeParam(
                                name = "T",
                                bounds = listOf(Nullable(TypeConstructor("kotlin.Any", emptyList())))
                            )
                        )
                    )
                    this.dri.packageName equals "property"
                    this.dri.classNames equals "XD"
                    this.dri.callable?.receiver equals receiver
                    this.dri.callable?.params equals listOf(
                        TypeParam(
                            name = "T",
                            bounds = listOf(
                                Nullable(
                                    TypeConstructor(
                                        "kotlin.Any",
                                        emptyList()
                                    )
                                )
                            )
                        )
                    )
                }
                generics counts 0
                visibility.values allEquals KotlinVisibility.Public
            }
        }
    }

    @Test
    fun `member properties and Java fields should have no generic params in kotlin`() {
        inlineModelTest(
            """
            |/src/sample/ParentInKotlin.kt
            |package sample
            |
            | class KtContainer<T> : MyContainer<T>() {
            |   val ktProp: T
            |}
            |
            |/src/sample/MyContainer.java
            |package sample;
            |
            |public class MyContainer<T> {
            |    public T prop;
            |}
            """.trimIndent()
        ) {
            with((this / "sample" / "KtContainer" / "prop").cast<DProperty>()) {
                generics counts 0
            }
            with((this / "sample" / "KtContainer" / "ktProp").cast<DProperty>()) {
                generics counts 0
            }
        }
    }

    @Test
    @OnlySymbols("context parameters")
    fun `property with context parameters should have them, correct DRI, and documentation`() {
        inlineModelTest(
            """
            |/src/sample/ParentInKotlin.kt
            |package sample
            |
            |/** Some doc */
            |context(s: String, _:Int) var Int.prop : Int
            """.trimIndent()
        ) {
            with((this / "sample" / "prop").cast<DProperty>()) {
                dri.callable equals org.jetbrains.dokka.links.Callable(
                    name = "prop",
                    receiver = TypeConstructor("kotlin.Int", emptyList()),
                    params = emptyList(),
                    contextParameters = listOf(
                        TypeConstructor("kotlin.String", emptyList()),
                        TypeConstructor("kotlin.Int", emptyList())
                    ),
                    isProperty = true
                )
                documentation.values.single().children.single().text() equals "Some doc\n"
                contextParameters counts 2
                contextParameters[0].name equals "s"
                contextParameters[1].name equals "_"
                with(contextParameters[0].type.assertNotNull("type")) {
                    name equals "String"
                }
                with(contextParameters[1].type.assertNotNull("type")) {
                    name equals "Int"
                }

                getter?.contextParameters counts 0
                setter?.contextParameters counts 0

                getter?.dri?.callable?.contextParameters counts 2
                setter?.dri?.callable?.contextParameters counts 2
            }
        }
    }
}
