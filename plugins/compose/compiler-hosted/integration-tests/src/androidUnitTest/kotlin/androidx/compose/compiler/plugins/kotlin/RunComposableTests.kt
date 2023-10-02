/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import java.net.URLClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.setupLanguageVersionSettings
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Test

class RunComposableTests(useFir: Boolean) : AbstractCodegenTest(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        setupLanguageVersionSettings(K2JVMCompilerArguments().apply {
            // enabling multiPlatform to use expect/actual declarations
            multiPlatform = true
        })
    }

    @Test // Bug report: https://github.com/JetBrains/compose-jb/issues/1407
    fun testSimpleDefaultInComposable() {
        // TODO: Enable once https://youtrack.jetbrains.com/issue/KT-56173 is fixed.
        assumeFalse(useFir)
        runCompose(
            testFunBody = """
                results["defaultValue"] = ExpectComposable()
                results["anotherValue"] = ExpectComposable("anotherValue")
                results["defaultValueDefaultTransform"] = ExpectComposableWithNonComposableLambda()
                results["anotherValueOtherTransform"] =
                    ExpectComposableWithNonComposableLambda("anotherValue") {
                        it + "OtherTransform"
                    }
            """.trimIndent(),
            commonFiles = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    expect fun ExpectComposable(
                        value: String = "defaultValue"
                    ): String

                    @Composable
                    expect fun ExpectComposableWithNonComposableLambda(
                        value: String = "defaultValue",
                        transform: (String) -> String = { it + "DefaultTransform" }
                    ): String
                """.trimIndent()
            ),
            platformFiles = mapOf("Actual.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    actual fun ExpectComposable(
                        value: String
                    ) = value

                    @Composable
                    actual fun ExpectComposableWithNonComposableLambda(
                        value: String,
                        transform: (String) -> String
                    ) = transform(value)
                """.trimIndent()
            )
        ) { results ->
            assertEquals("defaultValue", results["defaultValue"])
            assertEquals("anotherValue", results["anotherValue"])
            assertEquals("defaultValueDefaultTransform", results["defaultValueDefaultTransform"])
            assertEquals("anotherValueOtherTransform", results["anotherValueOtherTransform"])
        }
    }

    @Test // Bug report: https://github.com/JetBrains/compose-jb/issues/1407
    fun testDefaultValuesFromExpectComposableFunctions() {
        // TODO: Enable once https://youtrack.jetbrains.com/issue/KT-56173 and
        //       https://youtrack.jetbrains.com/issue/KT-58539 are fixed.
        assumeFalse(useFir)
        runCompose(
            testFunBody = """
                ExpectComposable { value ->
                    results["defaultValue"] = value
                }
                ExpectComposable("anotherValue") { value ->
                    results["anotherValue"] = value
                }
                results["returnDefaultValue"] = ExpectComposableWithReturn()
                results["returnAnotherValue"] = ExpectComposableWithReturn("returnAnotherValue")
            """.trimIndent(),
            commonFiles = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    expect fun ExpectComposable(
                        value: String = "defaultValue",
                        content: @Composable (v: String) -> Unit
                    )
                    @Composable
                    expect fun ExpectComposableWithReturn(
                        value: String = "returnDefaultValue"
                    ): String
                """.trimIndent()
            ),
            platformFiles = mapOf("Actual.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    actual fun ExpectComposable(
                        value: String,
                        content: @Composable (v: String) -> Unit
                    ) {
                        content(value)
                    }
                    @Composable
                    actual fun ExpectComposableWithReturn(
                        value: String
                    ): String = value
                """.trimIndent()
            )
        ) { results ->
            assertEquals("defaultValue", results["defaultValue"])
            assertEquals("anotherValue", results["anotherValue"])
            assertEquals("returnDefaultValue", results["returnDefaultValue"])
            assertEquals("returnAnotherValue", results["returnAnotherValue"])
        }
    }

    @Test
    fun testExpectWithGetExpectedPropertyInDefaultValueExpression() {
        // TODO: Enable once https://youtrack.jetbrains.com/issue/KT-56173 and
        //       https://youtrack.jetbrains.com/issue/KT-58539 are fixed.
        assumeFalse(useFir)
        runCompose(
            testFunBody = """
                ExpectComposable { value ->
                    results["defaultValue"] = value
                }
                ExpectComposable({ expectedProperty + expectedProperty.reversed() }) { value ->
                    results["anotherValue"] = value
                }
            """.trimIndent(),
            commonFiles = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    expect val expectedProperty: String

                    @Composable
                    expect fun ExpectComposable(
                        value: () -> String = { expectedProperty },
                        content: @Composable (v: String) -> Unit
                    )
                """.trimIndent()),
            platformFiles = mapOf(
                "Actual.kt" to """
                    import androidx.compose.runtime.*

                    actual val expectedProperty = "actualExpectedProperty"

                    @Composable
                    actual fun ExpectComposable(
                        value: () -> String,
                        content: @Composable (v: String) -> Unit
                    ) {
                        content(value())
                    }
                """.trimIndent()
            )
        ) { results ->
            assertEquals("actualExpectedProperty", results["defaultValue"])
            assertEquals(
                "actualExpectedProperty" + "actualExpectedProperty".reversed(),
                results["anotherValue"]
            )
        }
    }

    @Test
    fun testExpectWithComposableExpressionInDefaultValue() {
        // TODO: Enable once https://youtrack.jetbrains.com/issue/KT-56173 and
        //       https://youtrack.jetbrains.com/issue/KT-58539 are fixed.
        assumeFalse(useFir)
        runCompose(
            testFunBody = """
                ExpectComposable { value ->
                    results["defaultValue"] = value
                }
                ExpectComposable("anotherValue") { value ->
                    results["anotherValue"] = value
                }
            """.trimIndent(),
            commonFiles = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    fun defaultValueComposable(): String {
                        return "defaultValueComposable"
                    }

                    @Composable
                    expect fun ExpectComposable(
                        value: String = defaultValueComposable(),
                        content: @Composable (v: String) -> Unit
                    )
                """.trimIndent()),
            platformFiles = mapOf(
                "Actual.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    actual fun ExpectComposable(
                        value: String,
                        content: @Composable (v: String) -> Unit
                    ) {
                        content(value)
                    }
                """.trimIndent()
            )
        ) { results ->
            assertEquals("defaultValueComposable", results["defaultValue"])
            assertEquals("anotherValue", results["anotherValue"])
        }
    }

    @Test
    fun testExpectWithTypedParameter() {
        // TODO: Enable once https://youtrack.jetbrains.com/issue/KT-56173 and
        //       https://youtrack.jetbrains.com/issue/KT-58539 are fixed.
        assumeFalse(useFir)
        runCompose(
            testFunBody = """
                ExpectComposable<String>("aeiouy") { value ->
                    results["defaultValue"] = value
                }
                ExpectComposable<String>("aeiouy", { "anotherValue" }) { value ->
                    results["anotherValue"] = value
                }
            """.trimIndent(),
            commonFiles = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    expect fun <T> ExpectComposable(
                        value: T,
                        composeValue: @Composable () -> T = { value },
                        content: @Composable (T) -> Unit
                    )
                """.trimIndent()),
            platformFiles = mapOf(
                "Actual.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    actual fun <T> ExpectComposable(
                        value: T,
                        composeValue: @Composable () -> T,
                        content: @Composable (T) -> Unit
                    ) {
                        content(composeValue())
                    }
                """.trimIndent()
            )
        ) { results ->
            assertEquals("aeiouy", results["defaultValue"])
            assertEquals("anotherValue", results["anotherValue"])
        }
    }

    @Test
    fun testExpectWithRememberInDefaultValueExpression() {
        // TODO: Enable once https://youtrack.jetbrains.com/issue/KT-56173 and
        //       https://youtrack.jetbrains.com/issue/KT-58539 are fixed.
        assumeFalse(useFir)
        runCompose(
            testFunBody = """
                ExpectComposable { value ->
                    results["defaultValue"] = value
                }
                ExpectComposable(remember { "anotherRememberedValue" }) { value ->
                    results["anotherValue"] = value
                }
            """.trimIndent(),
            commonFiles = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    expect fun ExpectComposable(
                        value: String = remember { "rememberedDefaultValue" },
                        content: @Composable (v: String) -> Unit
                    )
                """.trimIndent()),
            platformFiles = mapOf(
                "Actual.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    actual fun ExpectComposable(
                        value: String,
                        content: @Composable (v: String) -> Unit
                    ) {
                        content(value)
                    }
                """.trimIndent()
            )
        ) { results ->
            assertEquals("rememberedDefaultValue", results["defaultValue"])
            assertEquals("anotherRememberedValue", results["anotherValue"])
        }
    }

    @Test
    fun testExpectWithDefaultValueUsingAnotherArgument() {
        // TODO: Enable once https://youtrack.jetbrains.com/issue/KT-56173 and
        //       https://youtrack.jetbrains.com/issue/KT-58539 are fixed.
        assumeFalse(useFir)
        runCompose(
            testFunBody = """
                ExpectComposable("AbccbA") { value ->
                    results["defaultValue"] = value
                }
                ExpectComposable("123", { s -> s + s.reversed() }) { value ->
                    results["anotherValue"] = value
                }
            """.trimIndent(),
            commonFiles = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    expect fun ExpectComposable(
                        value: String,
                        composeText: (String) -> String = { value },
                        content: @Composable (v: String) -> Unit
                    )
                """.trimIndent()),
            platformFiles = mapOf(
                "Actual.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    actual fun ExpectComposable(
                        value: String,
                        composeText: (String) -> String,
                        content: @Composable (v: String) -> Unit
                    ) {
                        content(composeText(value))
                    }
                """.trimIndent()
            )
        ) { results ->
            assertEquals("AbccbA", results["defaultValue"])
            assertEquals("123321", results["anotherValue"])
        }
    }

    @Test
    fun testNonComposableFunWithComposableParam() {
        // TODO: Enable once https://youtrack.jetbrains.com/issue/KT-56173 and
        //       https://youtrack.jetbrains.com/issue/KT-58539 are fixed.
        assumeFalse(useFir)
        runCompose(
            testFunBody = """
                savedContentLambda = null
                ExpectFunWithComposableParam { value ->
                    results["defaultValue"] = value
                }
                savedContentLambda!!.invoke()

                savedContentLambda = null
                ExpectFunWithComposableParam("3.14") { value ->
                    results["anotherValue"] = value
                }
                savedContentLambda!!.invoke()
            """.trimIndent(),
            commonFiles = mapOf(
                "Expect.kt" to """
                    import androidx.compose.runtime.*

                    var savedContentLambda: (@Composable () -> Unit)? = null

                    expect fun ExpectFunWithComposableParam(
                        value: String = "000",
                        content: @Composable (v: String) -> Unit
                    )
                """.trimIndent()),
            platformFiles = mapOf(
                "Actual.kt" to """
                    import androidx.compose.runtime.*

                    actual fun ExpectFunWithComposableParam(
                        value: String,
                        content: @Composable (v: String) -> Unit
                    ) {
                        savedContentLambda = {
                            content(value)
                        }
                    }
                """.trimIndent()
            )
        ) { results ->
            assertEquals("000", results["defaultValue"])
            assertEquals("3.14", results["anotherValue"])
        }
    }

    // This method was partially borrowed/copy-pasted from RobolectricComposeTester
    // where some of the code was commented out. Those commented out parts are needed here.
    private fun runCompose(
        @Language("kotlin")
        mainImports: String = "",
        @Language("kotlin")
        testFunBody: String,
        commonFiles: Map<String, String>, // name to source code
        platformFiles: Map<String, String>, // name to source code
        accessResults: (results: HashMap<*, *>) -> Unit
    ) {
        val className = "TestFCS_${uniqueNumber++}"

        val allCommonSources = commonFiles + ("Main.kt" to """
            import androidx.compose.runtime.*
            $mainImports

            class $className {
                val results = hashMapOf<String, Any>()

                @Composable
                fun test() {
                    $testFunBody
                }
            }

        """.trimIndent())

        val compiledClasses = classLoader(platformFiles, allCommonSources)
        val allClassFiles = compiledClasses.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }

        val loader = URLClassLoader(emptyArray(), this.javaClass.classLoader)

        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClassFiles) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(loader, null, bytes)
                if (loadedClass.name == className) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $className in loaded classes")
        }

        val instanceOfClass = instanceClass.getDeclaredConstructor().newInstance()
        val testMethod = instanceClass.getMethod(
            "test",
            *emptyArray(),
            Composer::class.java,
            Int::class.java
        )
        val getResultsMethod = instanceClass.getMethod("getResults")

        val setContentMethod = Composition::class.java.methods.first { it.name == "setContent" }
        setContentMethod.isAccessible = true

        val realComposable: (Composer, Int) -> Unit = { composer, _ ->
            testMethod.invoke(instanceOfClass, *emptyArray(), composer, 1)
        }

        val composition = Composition(UnitApplier(), createRecomposer())
        setContentMethod.invoke(composition, realComposable)

        val results = getResultsMethod.invoke(instanceOfClass) as HashMap<*, *>
        accessResults(results)
    }

    private class UnitApplier : Applier<Unit> {
        override val current: Unit
            get() = Unit

        override fun down(node: Unit) {}
        override fun up() {}
        override fun insertTopDown(index: Int, instance: Unit) {}
        override fun insertBottomUp(index: Int, instance: Unit) {}
        override fun remove(index: Int, count: Int) {}
        override fun move(from: Int, to: Int, count: Int) {}
        override fun clear() {}
    }

    private object SixtyFpsMonotonicFrameClock : MonotonicFrameClock {
        private const val fps = 60

        override suspend fun <R> withFrameNanos(
            onFrame: (Long) -> R
        ): R {
            delay(1000L / fps)
            return onFrame(System.nanoTime())
        }
    }

    private fun createRecomposer(): Recomposer {
        val mainScope = CoroutineScope(
            NonCancellable + Dispatchers.Main + SixtyFpsMonotonicFrameClock
        )

        return Recomposer(mainScope.coroutineContext).also {
            mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                it.runRecomposeAndApplyChanges()
            }
        }
    }
}
