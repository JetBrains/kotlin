/*
 * Copyright 2023 The Android Open Source Project
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

import org.junit.Test

class FunctionalInterfaceTransformTests(
    useFir: Boolean
) : AbstractControlFlowTransformTests(useFir) {
    @Test
    fun testFunctionalInterfaceWithExtensionReceiverTransformation() {
        verifyGoldenComposeIrTransform(
            source = """
                import androidx.compose.runtime.*
                fun interface TestContent {
                    @Composable
                    fun String.Content()
                }
                @Composable
                fun Test(content: TestContent) {
                    with(content) {
                        "".Content()
                    }
                }

                @Composable
                fun CallTest() {
                    Test { this.length }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testFunInterfaces() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface A {
                fun compute(value: Int): Unit
            }

            @Composable
            fun Example(a: A) {
                Example { it -> a.compute(it) }
            }
        """
    )

    @Test
    fun testComposableFunInterfaces() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface A {
                @Composable fun compute(value: Int): Unit
            }
            fun Example(a: A) {
                Example { it -> a.compute(it) }
            }
        """
    )

    @Test
    fun testComposableFunInterfacesInVariance() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface Consumer<T> {
                @Composable fun consume(t: T)
            }

            class Repro<T : Any> {
                fun test(consumer: Consumer<in T>) {}
            }

            fun test() {
                Repro<String>().test { string ->
                    println(string)
                }
            }
        """
    )

    @Test
    fun testCaptureStableFunInterface() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface Consumer {
                fun consume(t: Int)
            }

            @Composable fun Test(int: Int) {
                Example {
                    println(int)
                }
            }

            @Composable inline fun Example(consumer: Consumer) {
            }
            """
    )

    @Test
    fun testNoCaptureFunInterface() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface Consumer {
                fun consume(t: Int)
            }

            @Composable fun Test(int: Int) {
                Example {
                    println(it)
                }
            }

            @Composable inline fun Example(consumer: Consumer) {
            }
        """
    )

    @Test
    fun testComposableFunInterfaceWAnonymousParam() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface Consumer {
                @Composable operator fun invoke(t: Int)
            }

            @Composable fun Test(int: Int) {
                Example { _ ->
                }
            }

            @Composable fun Example(consumer: Consumer) {
            }
        """
    )

    @Test
    fun testComposableFunInterfaceWComposableLambda() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            private fun Decorated(boolean: Boolean) {
                Decoratable(decorator = { content ->
                    used(boolean)
                    content()
                })
            }

            @Composable
            private fun Decoratable(decorator: Decoration) {
                decorator.Decoration {

                }
            }

            fun interface Decoration {
                @Composable fun Decoration(content: @Composable () -> Unit)
            }
        """,
        """
            fun used(any: Any?) {}
        """
    )

    @Test
    fun testComposableFunInterfaceWComposableLambdaCaptureVariable() =
        verifyGoldenComposeIrTransform(
            """
            import androidx.compose.runtime.*

            @Composable
            private fun Decorated(boolean: Boolean) {
                var something = boolean
                Decoratable(decorator = { content ->
                    used(something)
                    content()
                })
            }

            @Composable
            private fun Decoratable(decorator: Decoration) {
                decorator.Decoration {

                }
            }

            fun interface Decoration {
                @Composable fun Decoration(content: @Composable () -> Unit)
            }
        """,
            """
            fun used(any: Any?) {}
        """
        )
}
