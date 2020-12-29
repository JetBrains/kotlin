package model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.KotlinModifier.*
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull
import utils.name
import utils.supers


class ClassesTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "classes") {

    @Test
    fun emptyClass() {
        inlineModelTest(
            """
            |class Klass {}"""
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 4
            }
        }
    }

    @Test
    fun emptyObject() {
        inlineModelTest(
            """
            |object Obj {}
            """
        ) {
            with((this / "classes" / "Obj").cast<DObject>()) {
                name equals "Obj"
                children counts 3
            }
        }
    }

    @Test
    fun classWithConstructor() {
        inlineModelTest(
            """
            |class Klass(name: String)
        """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 4

                with(constructors.firstOrNull().assertNotNull("Constructor")) {
                    visibility.values allEquals KotlinVisibility.Public
                    parameters counts 1
                    with(parameters.firstOrNull().assertNotNull("Constructor parameter")) {
                        name equals "name"
                        type.name equals "String"
                    }
                }

            }
        }
    }

    @Test
    fun classWithFunction() {
        inlineModelTest(
            """
            |class Klass {
            |   fun fn() {}
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 5

                with((this / "fn").cast<DFunction>()) {
                    type.name equals "Unit"
                    parameters counts 0
                    visibility.values allEquals KotlinVisibility.Public
                }
            }
        }
    }

    @Test
    fun classWithProperty() {
        inlineModelTest(
            """
            |class Klass {
            |   val name: String = ""
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 5

                with((this / "name").cast<DProperty>()) {
                    name equals "name"
                    // TODO property name
                }
            }
        }
    }

    @Test
    fun classWithCompanionObject() {
        inlineModelTest(
            """
            |class Klass() {
            |   companion object {
            |        val x = 1
            |        fun foo() {}
            |    }
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 5

                with((this / "Companion").cast<DObject>()) {
                    name equals "Companion"
                    children counts 5

                    with((this / "x").cast<DProperty>()) {
                        name equals "x"
                    }

                    with((this / "foo").cast<DFunction>()) {
                        name equals "foo"
                        parameters counts 0
                        type.name equals "Unit"
                    }
                }
            }
        }
    }

    @Test
    fun dataClass() {
        inlineModelTest(
            """
                |data class Klass() {}
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                visibility.values allEquals KotlinVisibility.Public
                with(extra[AdditionalModifiers]!!.content.entries.single().value.assertNotNull("Extras")) {
                    this counts 1
                    first() equals ExtraModifiers.KotlinOnlyModifiers.Data
                }
            }
        }
    }

    @Test
    fun sealedClass() {
        inlineModelTest(
            """
                |sealed class Klass() {}
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                modifier.values.forEach { it equals Sealed }
            }
        }
    }

    @Test
    fun annotatedClassWithAnnotationParameters() {
        inlineModelTest(
            """
                |@Deprecated("should no longer be used") class Foo() {}
                """
        ) {
            with((this / "classes" / "Foo").cast<DClass>()) {
                with(extra[Annotations]!!.directAnnotations.entries.single().value.assertNotNull("Annotations")) {
                    this counts 1
                    with(first()) {
                        dri.classNames equals "Deprecated"
                        params.entries counts 1
                        (params["message"].assertNotNull("message") as StringValue).value equals "should no longer be used"
                    }
                }
            }
        }
    }

    @Test
    fun notOpenClass() {
        inlineModelTest(
            """
                |open class C() {
                |    open fun f() {}
                |}
                |
                |class D() : C() {
                |    override fun f() {}
                |}
                """
        ) {
            val C = (this / "classes" / "C").cast<DClass>()
            val D = (this / "classes" / "D").cast<DClass>()

            with(C) {
                modifier.values.forEach { it equals Open }
                with((this / "f").cast<DFunction>()) {
                    modifier.values.forEach { it equals Open }
                }
            }
            with(D) {
                modifier.values.forEach { it equals Final }
                with((this / "f").cast<DFunction>()) {
                    modifier.values.forEach { it equals Open }
                }
                D.supertypes.flatMap { it.component2() }.firstOrNull()?.typeConstructor?.dri equals C.dri
            }
        }
    }

    @Test
    fun indirectOverride() {
        inlineModelTest(
            """
                |abstract class C() {
                |    abstract fun foo()
                |}
                |
                |abstract class D(): C()
                |
                |class E(): D() {
                |    override fun foo() {}
                |}
                """
        ) {
            val C = (this / "classes" / "C").cast<DClass>()
            val D = (this / "classes" / "D").cast<DClass>()
            val E = (this / "classes" / "E").cast<DClass>()

            with(C) {
                modifier.values.forEach { it equals Abstract }
                ((this / "foo").cast<DFunction>()).modifier.values.forEach { it equals Abstract }
            }

            with(D) {
                modifier.values.forEach { it equals Abstract }
            }

            with(E) {
                modifier.values.forEach { it equals Final }

            }
            D.supers.single().typeConstructor.dri equals C.dri
            E.supers.single().typeConstructor.dri equals D.dri
        }
    }

    @Test
    fun innerClass() {
        inlineModelTest(
            """
                |class C {
                |    inner class D {}
                |}
                """
        ) {
            with((this / "classes" / "C").cast<DClass>()) {

                with((this / "D").cast<DClass>()) {
                    with(extra[AdditionalModifiers]!!.content.entries.single().value.assertNotNull("AdditionalModifiers")) {
                        this counts 1
                        first() equals ExtraModifiers.KotlinOnlyModifiers.Inner
                    }
                }
            }
        }
    }

    @Test
    fun companionObjectExtension() {
        inlineModelTest(
            """
                |class Klass {
                |    companion object Default {}
                |}
                |
                |/**
                | * The def
                | */
                |val Klass.Default.x: Int get() = 1
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"

                with((this / "Default").cast<DObject>()) {
                    name equals "Default"
                    // TODO extensions
                }
            }
        }
    }

//    @Test fun companionObjectExtension() {
//        checkSourceExistsAndVerifyModel("testdata/classes/companionObjectExtension.kt", defaultModelConfig) { model ->
//            val pkg = model.members.single()
//            val cls = pkg.members.single { it.name == "Foo" }
//            val extensions = cls.extensions.filter { it.kind == NodeKind.CompanionObjectProperty }
//            assertEquals(1, extensions.size)
//        }
//    }

    @Test
    fun secondaryConstructor() {
        inlineModelTest(
            """
                |class C() {
                |    /** This is a secondary constructor. */
                |    constructor(s: String): this() {}
                |}
                """
        ) {
            with((this / "classes" / "C").cast<DClass>()) {
                name equals "C"
                constructors counts 2

                constructors.map { it.name } allEquals "C"

                with(constructors.find { it.parameters.isNullOrEmpty() } notNull "C()") {
                    parameters counts 0
                }

                with(constructors.find { it.parameters.isNotEmpty() } notNull "C(String)") {
                    parameters counts 1
                    with(parameters.firstOrNull() notNull "Constructor parameter") {
                        name equals "s"
                        type.name equals "String"
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
                | * Useful
                | */
                |@SinceKotlin("1.1")
                |class C
                """
        ) {
            with((this / "classes" / "C").cast<DClass>()) {
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
    fun privateCompanionObject() {
        inlineModelTest(
            """
                |class Klass {
                |    private companion object {
                |        fun fn() {}
                |        val a = 0
                |    }
                |}
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                assertNull(companion, "Companion should not be visible by default")
            }
        }
    }

    @Test
    fun companionObject() {
        inlineModelTest(
            """
                |class Klass {
                |    companion object {
                |        fun fn() {}
                |        val a = 0
                |    }
                |}
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                with((this / "Companion").cast<DObject>()) {
                    name equals "Companion"
                    visibility.values allEquals KotlinVisibility.Public

                    with((this / "fn").cast<DFunction>()) {
                        name equals "fn"
                        parameters counts 0
                        receiver equals null
                    }
                }
            }
        }
    }

    @Test
    fun annotatedClass() {
        inlineModelTest(
            """@Suppress("abc") class Foo() {}"""
        ) {
            with((this / "classes" / "Foo").cast<DClass>()) {
                with(extra[Annotations]?.directAnnotations?.values?.firstOrNull()?.firstOrNull().assertNotNull("annotations")) {
                    dri.toString() equals "kotlin/Suppress///PointingToDeclaration/"
                    (params["names"].assertNotNull("param") as ArrayValue).value equals listOf(StringValue("abc"))
                }
            }
        }
    }

    @Test
    fun javaAnnotationClass() {
        inlineModelTest(
            """
                |import java.lang.annotation.Retention
                |import java.lang.annotation.RetentionPolicy
                |
                |@Retention(RetentionPolicy.SOURCE)
                |public annotation class throws()
            """
        ) {
            with((this / "classes" / "throws").cast<DAnnotation>()) {
                with(extra[Annotations]!!.directAnnotations.entries.single().value.assertNotNull("Annotations")) {
                    this counts 1
                    with(first()) {
                        dri.classNames equals "Retention"
                        params["value"].assertNotNull("value") equals EnumValue(
                            "RetentionPolicy.SOURCE",
                            DRI("java.lang.annotation", "RetentionPolicy.SOURCE")
                        )
                    }
                }
            }
        }
    }

    @Test
    fun genericAnnotationClass() {
        inlineModelTest(
            """annotation class Foo<A,B,C,D:Number>() {}"""
        ) {
            with((this / "classes" / "Foo").cast<DAnnotation>()) {
                generics.map { it.name to it.bounds.first().name } equals listOf(
                    "A" to "Any",
                    "B" to "Any",
                    "C" to "Any",
                    "D" to "Number"
                )
            }
        }
    }

    @Test
    fun nestedGenericClasses() {
        inlineModelTest(
            """
            |class Outer<OUTER> {
            |   inner class Inner<INNER, T : OUTER> { }
            |}
        """.trimMargin()
        ) {
            with((this / "classes" / "Outer").cast<DClass>()) {
                val inner = classlikes.single().cast<DClass>()
                inner.generics.map { it.name to it.bounds.first().name } equals listOf("INNER" to "Any", "T" to "OUTER")
            }
        }
    }

    @Test
    fun allImplementedInterfaces() {
        inlineModelTest(
            """
                | interface Highest { }
                | open class HighestImpl: Highest { }
                | interface Lower { }
                | interface LowerImplInterface: Lower { }
                | class Tested : HighestImpl(), LowerImplInterface { }
            """.trimIndent()
        ) {
            with((this / "classes" / "Tested").cast<DClass>()) {
                extra[ImplementedInterfaces]?.interfaces?.entries?.single()?.value?.map { it.dri.sureClassNames }
                    ?.sorted() equals listOf("Highest", "Lower", "LowerImplInterface").sorted()
            }
        }
    }

    @Test
    fun multipleClassInheritance() {
        inlineModelTest(
            """
                | open class A { }
                | open class B: A() { }
                | class Tested : B() { }
            """.trimIndent()
        ) {
            with((this / "classes" / "Tested").cast<DClass>()) {
                supertypes.entries.single().value.map { it.typeConstructor.dri.sureClassNames }.single() equals "B"
            }
        }
    }

    @Test
    fun multipleClassInheritanceWithInterface() {
        inlineModelTest(
            """
               | open class A { }
               | open class B: A() { }
               | interface X { }
               | interface Y : X { }
               | class Tested : B(), Y { }
            """.trimIndent()
        ) {
            with((this / "classes" / "Tested").cast<DClass>()) {
                supertypes.entries.single().value.map { it.typeConstructor.dri.sureClassNames to it.kind }
                    .sortedBy { it.first } equals listOf(
                    "B" to KotlinClassKindTypes.CLASS,
                    "Y" to KotlinClassKindTypes.INTERFACE
                )
            }
        }
    }

    @Test
    fun doublyTypealiasedException() {
        inlineModelTest(
            """
               | typealias B = RuntimeException
               | typealias A = B
            """.trimMargin()
        ) {
            with((this / "classes" / "A").cast<DTypeAlias>()) {
                extra[ExceptionInSupertypes].assertNotNull("Typealias A should have ExceptionInSupertypes in its extra field")
            }
            with((this / "classes" / "B").cast<DTypeAlias>()) {
                extra[ExceptionInSupertypes].assertNotNull("Typealias B should have ExceptionInSupertypes in its extra field")
            }
        }
    }
}
