package model

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DInterface
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull

class InheritorsTest : AbstractModelTest("/src/main/kotlin/inheritors/Test.kt", "inheritors") {

    @Test
    fun simple() {
        inlineModelTest(
            """|interface A{}
               |class B() : A {}
            """.trimMargin(),
        ) {
            with((this / "inheritors" / "A").cast<DInterface>()) {
                val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                with(map.keys.also { it counts 1 }.find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }
                ) {
                    this counts 1
                    first().classNames equals "B"
                }
            }
        }
    }

    @Test
    fun sealed() {
        inlineModelTest(
            """|sealed class A {}
               |class B() : A() {}
               |class C() : A() {}
               |class D()
            """.trimMargin(),
        ) {
            with((this / "inheritors" / "A").cast<DClass>()) {
                val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                with(map.keys.also { it counts 1 }.find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }
                ) {
                    this counts 2
                    mapNotNull { it.classNames }.sorted() equals listOf("B", "C")
                }
            }
        }
    }

    @Test
    fun multiplatform() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("common/src/", "jvm/src/")
                    analysisPlatform = "jvm"
                }
                sourceSet {
                    sourceRoots = listOf("common/src/", "js/src/")
                    analysisPlatform = "js"
                }
            }
        }

        testInline(
            """
            |/common/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |interface A{}
            |/jvm/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |class B() : A {}
            |/js/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |class B() : A {}
            |class C() : A {}
        """.trimMargin(),
            configuration,
            cleanupOutput = false,
        ) {
            documentablesTransformationStage = { m ->
                with((m / "inheritors" / "A").cast<DInterface>()) {
                    val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                    with(map.keys.also { it counts 2 }) {
                        with(find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }) {
                            this counts 1
                            first().classNames equals "B"
                        }
                        with(find { it.analysisPlatform == Platform.js }.assertNotNull("js key").let { map[it]!! }) {
                            this counts 2
                            val classes = listOf("B", "C")
                            assertTrue(all { classes.contains(it.classNames) }, "One of subclasses missing in js" )
                        }
                    }

                }
            }
        }
    }
}
