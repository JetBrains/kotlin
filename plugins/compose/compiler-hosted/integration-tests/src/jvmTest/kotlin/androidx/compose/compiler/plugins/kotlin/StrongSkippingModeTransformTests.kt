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

import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import java.io.File
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runners.Parameterized

class StrongSkippingModeTransformTests(
    useFir: Boolean,
    private val intrinsicRememberEnabled: Boolean
) : AbstractIrTransformTest(useFir) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useFir = {0}, intrinsicRemember = {1}")
        fun data() = arrayOf<Any>(
            arrayOf(false, false),
            arrayOf(false, true),
            arrayOf(true, false),
            arrayOf(true, true)
        )
    }

    override fun CompilerConfiguration.updateConfiguration() {
        put(
            ComposeConfiguration.FEATURE_FLAGS,
            listOf(
                FeatureFlag.StrongSkipping.featureName,
                FeatureFlag.OptimizeNonSkippingGroups.featureName,
                FeatureFlag.IntrinsicRemember.name(intrinsicRememberEnabled)
            )
        )
    }

    @Test
    fun testSingleStableParam(): Unit = verifyMemoization(
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
    fun testSingleUnstableParam(): Unit = verifyMemoization(
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
    fun testSingleNullableUnstableParam(): Unit = verifyMemoization(
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
    fun testSingleOptionalUnstableParam(): Unit = verifyMemoization(
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
    fun testRuntimeStableParam(): Unit = verifyMemoization(
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
    fun testStableUnstableParams(): Unit = verifyMemoization(
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
    fun testStaticDefaultParam() = verifyMemoization(
        """
            @Composable
            fun A(i: Int, list: List<Int>? = null, set: Set<Int> = emptySet()) {}
        """,
        """
            @Composable
            fun Test(i: Int) {
                A(i)
            }
        """
    )

    @Test
    fun testMemoizingUnstableCapturesInLambda() = verifyMemoization(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun Test() {
                val foo = Foo(0)
                val lambda = { foo }
            }
        """
    )

    @Test
    fun testDontMemoizeLambda() = verifyMemoization(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            fun Lam(x: ()->Unit) { x() }
        """,
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
    fun testMemoizingUnstableFunctionParameterInLambda() = verifyMemoization(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """,
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
    fun testMemoizingComposableLambda() = verifyMemoization(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """,
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
    fun testMemoizingStableAndUnstableCapturesInLambda() = verifyMemoization(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """,
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
    fun testFunctionInterfaceMemorized() = verifyMemoization(
        """
            fun interface TestFunInterface {
                fun compute(value: Int)
            }
            fun use(@Suppress("UNUSED_PARAMETER") v: Int) {}
        """,
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
        """
    )

    @Test
    fun testVarArgs() = verifyMemoization(
        "",
        """
            @Composable fun Varargs(vararg ints: Int) {
            }
            @Composable fun Test() {
                Varargs(1, 2, 3)
            }
        """
    )

    @Test
    fun testRuntimeStableVarArgs() = verifyMemoization(
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

    @Test
    fun testUnstableReceiverFunctionReferenceMemoized() = verifyMemoization(
        """
            class Unstable(var qux: Int = 0) { fun method(arg1: Int) {} }
            val unstable = Unstable()
        """,
        """
            @Composable
            fun Something() {
                val x = unstable::method
            }
        """
    )

    @Test
    fun testUnstableExtensionReceiverFunctionReferenceMemoized() = verifyMemoization(
        """
            class Unstable(var foo: Int = 0)
            fun Unstable.method(arg1: Int) {}
            val unstable = Unstable()
        """,
        """
            @Composable
            fun Something() {
                val x = unstable::method
            }
        """
    )

    private fun verifyMemoization(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        dumpTree: Boolean = false
    ) {
        val source = """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.ReadOnlyComposable

            $checked
        """

        val extra = """
             import androidx.compose.runtime.Composable

            $unchecked
            fun used(x: Any?) {}
        """

        // verify that generated keys are path independent
        val module1 = dumpIrWithPath(source, extra, "/home/folder1")
        val module2 = dumpIrWithPath(source, extra, "/home/folder2")

        assertEquals(module1, module2)

        verifyGoldenComposeIrTransform(
            source,
            extra,
            dumpTree = dumpTree
        )
    }

    private fun dumpIrWithPath(
        source: String,
        extra: String,
        path: String,
    ): String {
        val sourceFile1 = SourceFile("Test.kt", source, path = path)
        val extraFile1 = SourceFile("Extra.kt", extra, path = path)
        return compileToIr(listOf(sourceFile1, extraFile1)).files.joinToString("\n") {
            buildString {
                val fileShortName = it.fileEntry.name.takeLastWhile { it != File.separatorChar }
                appendLine("IrFile: $fileShortName")
                it.acceptChildren(DumpIrTreeVisitor(this, DumpIrTreeOptions()), "")
            }
        }
    }
}
