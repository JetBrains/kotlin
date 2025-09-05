package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.util.assertContains
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KT75869DeprecatedConstructors {

    @Test
    fun testDeprecatedConstructorsThrowConfigurationTimeError() {
        val subclassedJsTest = runCatching {
            buildProjectWithMPP {
                @Suppress("DEPRECATION_ERROR")
                abstract class JsTestSubclass @Inject constructor(compilation: KotlinJsIrCompilation) : KotlinJsTest(compilation)

                kotlin {
                    tasks.register(
                        "foo",
                        JsTestSubclass::class.java,
                        js().compilations.getByName("main")
                    ).get()
                }
            }
        }
        val subclassingError = assertNotNull(subclassedJsTest.exceptionOrNull()?.stackTraceToString())
        assertContains(
            $$"Cannot create instance of org.jetbrains.kotlin.gradle.unitTests.KT75869DeprecatedConstructors$testDeprecatedConstructorsThrowConfigurationTimeError$subclassedJsTest$1$1$JsTestSubclass. Constructor is deprecated, see Kdoc for details.",
            subclassingError,
        )
    }
}