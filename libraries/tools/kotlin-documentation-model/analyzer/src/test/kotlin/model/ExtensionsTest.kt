/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.base.transformers.documentables.CallableExtensions
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import utils.AbstractModelTest
import kotlin.test.Test

class ExtensionsTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "classes") {
    private fun <T : WithExtraProperties<R>, R : Documentable> T.checkExtension(name: String = "extension") =
        with(extra[CallableExtensions]?.extensions) {
            this notNull "extensions"
            this counts 1
            (this?.single() as? DFunction)?.name equals name
        }

    @Test
    fun `should be extension for subclasses`() {
        inlineModelTest(
            """
            |open class A
            |open class B: A()
            |open class C: B()
            |open class D: C()
            |fun B.extension() = ""
            """
        ) {
            with((this / "classes" / "B").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "C").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "D").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "A").cast<DClass>()) {
                extra[CallableExtensions] equals null
            }
        }
    }

    @Test
    fun `should be extension for interfaces`() {
        inlineModelTest(
            """
            |interface I
            |interface I2 : I
            |open class A: I2
            |fun I.extension() = ""
            """
        ) {

            with((this / "classes" / "A").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "I2").cast<DInterface>()) {
                checkExtension()
            }
            with((this / "classes" / "I").cast<DInterface>()) {
                checkExtension()
            }
        }
    }

    @Test
    fun `should be extension for external classes`() {
        inlineModelTest(
            """
            |abstract class A<T>: AbstractList<T>()
            |fun<T> AbstractCollection<T>.extension() {}
            |
            |class B:Exception()
            |fun Throwable.extension() = ""
            """
        ) {
            with((this / "classes" / "A").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "B").cast<DClass>()) {
                checkExtension()
            }
        }
    }

    @Test
    fun `should be extension for typealias`() {
        inlineModelTest(
            """
            |open class A
            |open class B: A()
            |open class C: B()
            |open class D: C()
            |typealias B2 = B
            |fun B2.extension() = ""
            """
        ) {
            with((this / "classes" / "B").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "C").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "D").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "A").cast<DClass>()) {
                extra[CallableExtensions] equals null
            }
        }
    }

    @Test
    fun `should be extension for java classes`() {
        val testConfiguration = dokkaConfiguration {
            suppressObviousFunctions = false
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/")
                    classpath += jvmStdlibPath!!
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/classes/Test.kt
            | package classes
            | fun A.extension() = ""
            | 
            |/src/main/kotlin/classes/A.java
            | package classes;
            | public class A {}
            | 
            | /src/main/kotlin/classes/B.java
            | package classes;
            | public class B extends A {}
            """,
            configuration = testConfiguration
        ) {
            documentablesTransformationStage = {
                it.run {
                    with((this / "classes" / "B").cast<DClass>()) {
                        checkExtension()
                    }
                    with((this / "classes" / "A").cast<DClass>()) {
                        checkExtension()
                    }
                }
            }
        }
    }
}
