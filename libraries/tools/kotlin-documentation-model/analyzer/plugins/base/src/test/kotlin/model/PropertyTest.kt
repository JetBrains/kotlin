package model

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull
import utils.name
import kotlin.test.assertEquals

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

    @Test fun genericTopLevelExtensionProperty(){
        inlineModelTest(
            """ | val <T : Number> List<T>.sampleProperty: T
                |   get() { TODO() }
            """.trimIndent()
        ){
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

    @Test fun genericExtensionPropertyInClass(){
        inlineModelTest(
            """ | package test
                | class XD<T> {
                |   var List<T>.sampleProperty: T
                |       get() { TODO() }
                |       set(value) { TODO() }
                | }
            """.trimIndent()
        ){
            with((this / "property" / "XD" / "sampleProperty").cast<DProperty>()) {
                name equals "sampleProperty"
                children counts 0
                with(receiver.assertNotNull("Property receiver")) {
                    type.name equals "List"
                }
                with(getter.assertNotNull("Getter")) {
                    type.name equals "T"
                }
                with(setter.assertNotNull("Setter")){
                    type.name equals "Unit"
                }
                generics counts 0
                visibility.values allEquals KotlinVisibility.Public
            }
        }
    }
}
