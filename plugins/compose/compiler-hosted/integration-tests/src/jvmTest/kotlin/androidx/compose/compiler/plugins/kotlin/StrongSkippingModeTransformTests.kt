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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test

class StrongSkippingModeTransformTests(useFir: Boolean) :
    FunctionBodySkippingTransformTestsBase(useFir) {

    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_KEY, false)
        put(ComposeConfiguration.STRONG_SKIPPING_ENABLED_KEY, true)
    }

    @Test
    fun testSingleStableParam(): Unit = comparisonPropagation(
        """
            class Foo(val value: Int = 0)
            @Composable fun A(x: Foo) {}
        """,
        """
            @Composable
            fun Test(x: Foo) {
                A(x)
            }
        """
    )

    @Test
    fun testSingleUnstableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo) {}
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun Test(x: Foo) {
                A(x)
            }
        """
    )

    @Test
    fun testSingleNullableUnstableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo?) {}
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun Test(x: Foo?) {
                A(x)
            }
        """
    )

    @Test
    fun testSingleOptionalUnstableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo?) {}
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun Test(x: Foo? = Foo()) {
                A(x)
            }
        """
    )

    @Test
    fun testRuntimeStableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            class Holder<T> {
                @Composable
                fun Test(x: T) {
                    A(x as Int)
                }
            }
        """
    )

    @Test
    fun testStableUnstableParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
        """,
        """
            @Composable fun CanSkip(a: Int = 0, b: Foo = Foo()) {
                used(a)
                used(b)
            }
            @Composable fun CannotSkip(a: Int, b: Foo) {
                used(a)
                used(b)
                print("Hello World")
            }
            @Composable fun NoParams() {
                print("Hello World")
            }
        """
    )

    @Test
    fun testStaticDefaultParam() = comparisonPropagation(
        """
            @Composable
            fun A(i: Int, list: List<Int>? = null, set: Set<Int> = emptySet()) {}
        """.trimIndent(),
        """
            @Composable
            fun Test(i: Int) {
                A(i)
            }
        """.trimIndent()
    )

    @Test
    fun testMemoizingUnstableCapturesInLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
        """.trimIndent(),
        """
            @Composable
            fun Test() {
                val foo = Foo(0)
                val lambda = { foo }
            }
        """
    )

    @Test
    fun testDontMemoizeLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            fun Lam(x: ()->Unit) { x() }
        """.trimIndent(),
        """
            import androidx.compose.runtime.DontMemoize

            @Composable
            fun Test() {
                val foo = Foo(0)
                val lambda = @DontMemoize { foo }
                Lam @DontMemoize { foo }
            }
        """
    )

    @Test
    fun testMemoizingUnstableFunctionParameterInLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """.trimIndent(),
        """
            @Composable
            fun Test(foo: Foo, bar: Bar) {
                val lambda: ()->Unit = { 
                    foo
                    bar
                }
            }
        """
    )

    @Test
    fun testMemoizingComposableLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """.trimIndent(),
        """
            @Composable
            fun Test(foo: Foo, bar: Bar) {
                val lambda: @Composable ()->Unit = {
                    foo
                    bar
                }
            }
        """
    )

    @Test
    fun testMemoizingStableAndUnstableCapturesInLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """.trimIndent(),
        """
            @Composable
            fun Test() {
                val foo = Foo(0)
                val bar = Bar(1)
                val lambda = {
                    foo
                    bar
                }
            }
        """
    )

    @Test
    fun testFunctionInterfaceMemorized() = comparisonPropagation(
        """
            fun interface TestFunInterface {
                fun compute(value: Int)
            }
            fun use(@Suppress("UNUSED_PARAMETER") v: Int) {}
        """.trimIndent(),
        """
            @Composable fun TestMemoizedFun(compute: TestFunInterface) {}
            @Composable fun Test() {
                val capture = 0
                TestMemoizedFun {
                    // no captures
                    use(it)
                }
                TestMemoizedFun {
                    // stable captures
                    use(capture)
                }
            }
        """.trimIndent()
    )

    @Test
    fun testVarArgs() = comparisonPropagation(
        "",
        """
            @Composable fun Varargs(vararg ints: Int) {
            }
            @Composable fun Test() {
                Varargs(1, 2, 3)
            }
        """.trimIndent()
    )

    @Test
    fun testRuntimeStableVarArgs() = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            class Holder<T> {
                @Composable
                fun Test(vararg x: T) {
                    A(x as Int)
                }
            }
        """
    )
}
