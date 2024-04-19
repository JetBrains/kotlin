/*
 * Copyright 2020 The Android Open Source Project
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

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test

class RememberIntrinsicTransformTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, true)
        put(
            ComposeConfiguration.FEATURE_FLAGS,
            listOf(
                FeatureFlag.OptimizeNonSkippingGroups.featureName,
                FeatureFlag.IntrinsicRemember.featureName
            )
        )
    }

    private fun comparisonPropagation(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        dumpTree: Boolean = false
    ) = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember

            $checked
        """.trimIndent(),
        """
            import androidx.compose.runtime.Composable

            $unchecked
            fun used(x: Any?) {}
        """.trimIndent(),
        dumpTree = dumpTree
    )

    @Test
    // The intent of this test is incorrect. If a slot is conditional it requires a group
    // as slots can only be inserted and removed as part of a group. If we made the remember in the
    // `if` statement intrinsic when taking the "else" part of the branch will leave the cached
    // state as part of the slot table (preventing `onForgotten` from being called) as its group
    // was not removed (not calling `onForgotten`). Keeping for now to ensure we handle this case
    // correctly if intrinsic remember is allowed in more places.
    //
    // If the remember in the `if` statement was converted to be an intrinsic remember the else
    // block would need to explicitly overwrite the slots used in the `then` part with
    // [Composer.Empty]. Note that this also means that the else block takens more slot space as
    // the slots are still allocated but unused.
    //
    // Without intrinsic remember, the slots are deleted from the slot table when
    // the "remember" group is removed but at the cost of a group to track the slot.
    fun testElidedRememberInsideIfDeoptsRememberAfterIf(): Unit = comparisonPropagation(
        "",
        """
            import androidx.compose.runtime.NonRestartableComposable

            @Composable
            @NonRestartableComposable
            fun app(x: Boolean) {
                val a = if (x) { remember { 1 } } else { 2 }
                val b = remember { 2 }
            }
        """
    )

    @Test
    fun testMultipleParamInputs(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun <T> loadResourceInternal(
                key: String,
                pendingResource: T? = null,
                failedResource: T? = null
            ): Boolean {
                val deferred = remember(key, pendingResource, failedResource) {
                    123
                }
                return deferred > 10
            }
        """
    )

    @Test
    fun testRestartableParameterInputsStableUnstableUncertain(): Unit = comparisonPropagation(
        """
            class KnownStable
            class KnownUnstable(var x: Int)
            interface Uncertain
        """,
        """
            @Composable
            fun test1(x: KnownStable) {
                remember(x) { 1 }
            }
            @Composable
            fun test2(x: KnownUnstable) {
                remember(x) { 1 }
            }
            @Composable
            fun test3(x: Uncertain) {
                remember(x) { 1 }
            }
        """
    )

    @Test
    fun testNonRestartableParameterInputsStableUnstableUncertain(): Unit = comparisonPropagation(
        """
            class KnownStable
            class KnownUnstable(var x: Int)
            interface Uncertain
        """,
        """
            import androidx.compose.runtime.NonRestartableComposable

            @Composable
            @NonRestartableComposable
            fun test1(x: KnownStable) {
                remember(x) { 1 }
            }
            @Composable
            @NonRestartableComposable
            fun test2(x: KnownUnstable) {
                remember(x) { 1 }
            }
            @Composable
            @NonRestartableComposable
            fun test3(x: Uncertain) {
                remember(x) { 1 }
            }
        """
    )

    @Test
    fun testPassedArgs(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
        """,
        """
            @Composable
            fun rememberFoo(a: Int, b: Int) = remember(a, b) { Foo(a, b) }
        """
    )

    @Test
    fun testNoArgs(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A(){}
        """,
        """
            @Composable
            fun Test() {
                val foo = remember { Foo() }
                val bar = remember { Foo() }
                A()
                val bam = remember { Foo() }
            }
        """
    )

    @Test
    fun testNonArgs(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test() {
                val a = someInt()
                val b = someInt()
                val foo = remember(a, b) { Foo(a, b) }
            }
        """
    )

    @Test
    fun testComposableCallInArgument(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun CInt(): Int { return 123 }
        """,
        """
            @Composable
            fun Test() {
                val foo = remember(CInt()) { Foo() }
            }
        """
    )

    @Test
    fun testCompositionLocalCallBeforeRemember(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.compositionLocalOf

            class Foo
            class Bar
            val compositionLocalBar = compositionLocalOf<Bar> { Bar() }
        """,
        """
            @Composable
            fun Test() {
                val bar = compositionLocalBar.current
                val foo = remember(bar) { Foo() }
            }
        """
    )

    @Test
    fun testCompositionLocalCallAsInput(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.compositionLocalOf

            class Foo
            class Bar
            val compositionLocalBar = compositionLocalOf<Bar> { Bar() }
        """,
        """
            @Composable
            fun Test() {
                val foo = remember(compositionLocalBar.current) { Foo() }
            }
        """
    )

    @Test
    fun testComposableCallBeforeRemember(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A() { }
        """,
        """
            @Composable
            fun Test() {
                A()
                val foo = remember { Foo() }
            }
        """
    )

    @Test
    fun testRememberInsideOfIf(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test(condition: Boolean) {
                A()
                if (condition) {
                    val foo = remember { Foo() }
                }
            }
        """
    )

    @Test
    fun testRememberInsideOfIfWithComposableCallBefore(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test(condition: Boolean) {
                if (condition) {
                    A()
                    val foo = remember { Foo() }
                }
            }
        """
    )

    @Test
    fun testRememberInsideOfWhileWithOnlyRemembers(): Unit = comparisonPropagation(
        """
            class Foo
        """,
        """
            @Composable
            fun Test(items: List<Int>) {
                for (item in items) {
                    val foo = remember { Foo() }
                    print(foo)
                    print(item)
                }
            }
        """
    )

    @Test
    fun testRememberInsideOfWhileWithCallsAfter(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test(items: List<Int>) {
                for (item in items) {
                    val foo = remember { Foo() }
                    A()
                    print(foo)
                    print(item)
                }
            }
        """
    )

    @Test
    fun testZeroArgRemember(): Unit = comparisonPropagation(
        """
            class Foo
        """,
        """
            @Composable
            fun Test(items: List<Int>) {
                val foo = remember { Foo() }
                used(items)
            }
        """
    )

    @Test
    fun testRememberWithNArgs(): Unit = comparisonPropagation(
        """
            class Foo
            class Bar
        """,
        """
            @Composable
            fun Test(a: Int, b: Int, c: Bar, d: Boolean) {
                val foo = remember(a, b, c, d) { Foo() }
            }
        """
    )

    @Test
    fun testVarargWithSpread(): Unit = comparisonPropagation(
        """
            class Foo
            class Bar
        """,
        """
            @Composable
            fun Test(items: Array<Bar>) {
                val foo = remember(*items) { Foo() }
            }
        """
    )

    @Test
    fun testRememberWithInlineClassInput(): Unit = comparisonPropagation(
        """
            class Foo
            inline class InlineInt(val value: Int)
        """,
        """
            @Composable
            fun Test(inlineInt: InlineInt) {
                val a = InlineInt(123)
                val foo = remember(inlineInt, a) { Foo() }
            }
        """
    )

    @Test
    fun testMultipleRememberCallsInARow(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test() {
                val a = someInt()
                val b = someInt()
                val foo = remember(a, b) { Foo(a, b) }
                val c = someInt()
                val d = someInt()
                val bar = remember(c, d) { Foo(c, d) }
            }
        """
    )

    @Test
    fun testParamAndNonParamInputsInRestartableFunction(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test(a: Int) {
                val b = someInt()
                val foo = remember(a, b) { Foo(a, b) }
            }
        """
    )

    @Test
    fun testParamAndNonParamInputsInDirectFunction(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test(a: Int): Foo {
                val b = someInt()
                return remember(a, b) { Foo(a, b) }
            }
        """
    )

    @Test
    fun testRememberMemoizedLambda(): Unit = comparisonPropagation(
        "",
        """
            @Composable
            fun Test(a: Int) {
                used { a }
            }
        """
    )

    @Test
    fun testRememberFunctionReference(): Unit = comparisonPropagation(
        """
            fun effect(): Int = 0
        """,
        """
            @Composable
            fun Test(a: Int) {
                used(remember(a, ::effect))
            }
        """
    )

    @Test
    fun testRememberAdaptedFunctionReference(): Unit = comparisonPropagation(
        """
            fun effect(a: Int = 0): Int = a
        """,
        """
            @Composable
            fun Test(a: Int) {
                used(remember(a, ::effect))
            }
        """
    )

    @Test
    fun testRememberPropertyReference(): Unit = comparisonPropagation(
        """
            class A(val value: Int)
        """.trimIndent(),
        """
            @Composable
            fun Test(a: A) {
                used(remember(a, a::value))
            }
        """
    )

    @Test
    fun testOptimizationFailsIfDefaultsGroupIsUsed(): Unit = comparisonPropagation(
        """
            class Foo
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test(a: Int = someInt()) {
                val foo = remember { Foo() }
                used(foo)
                used(a)
            }
        """
    )

    @Test
    fun testIntrinsicRememberOfDefaultParameters_Simple() = comparisonPropagation(
        """""",
        """
            @Composable
            fun Test(a: Int = remember { 0 }) {
                used(a)
            }
        """
    )

    @Test
    fun testIntrinsicRememberOfDefaultParameters_AfterComposable() = comparisonPropagation(
        """
            @Composable
            fun SomeComposable() = 0
        """,
        """
            @Composable
            fun Test(a: Int = remember { 0 }, b: Int = SomeComposable(), c: Int = remember { 0 }) {
                used(a)
                used(b)
                used(c)
            }
        """
    )

    @Test
    fun testIntrinsicRememberOfLambdaInIfBlock() = comparisonPropagation(
        // Simulation of Scrim in BackdropScaffold
        """
            class Modifier
            fun Modifier.pointerInput(key1: Any?, block: () -> Unit) = this
            fun detectTapGestures(block: () -> Unit) {}
            @Composable fun someComposableValue(): Int = 1
        """,
        """
        @Composable
        fun Test(a: Boolean, visible: Boolean, onDismiss: () -> Unit) {
            if (a) {
                val a = someComposableValue()
                used(a)
                val m = Modifier()
                val dismissModifier = if (visible) {
                    m.pointerInput(Unit) { detectTapGestures { onDismiss() } }
                } else {
                    m
                }
                used(dismissModifier)
            }
        }
        """
    )

    @Test
    fun testRememberAfterStaticDefaultParameters() = comparisonPropagation(
        unchecked = """
            import androidx.compose.runtime.*

            enum class Foo {
                A,
                B,
                C,
            }

            @Stable
            fun swizzle(a: Int, b: Int) = a + b

            @Composable
            fun used(a: Any) { }
            """,
        checked = """
            @Composable
            fun Test(a: Int = 1, b: Foo = Foo.B, c: Int = swizzle(1, 2) ) {
                val s = remember(a, b, c) { Any() }
                used(s)
            }
        """
    )

    @Test
    fun testRememberAfterNonStaticDefaultParameters() = comparisonPropagation(
        unchecked = """
            import androidx.compose.runtime.*

            enum class Foo {
                A,
                B,
                C,
            }

            fun swizzle(a: Int, b: Int) = a + b

            @Composable
            fun used(a: Any) { }
            """,
        checked = """
            @Composable
            fun Test(a: Int = 1, b: Foo = Foo.B, c: Int = swizzle(1, 2) ) {
                val s = remember(a, b, c) { Any() }
                used(s)
            }
        """
    )

    @Test
    fun testForEarlyExit() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(condition: Boolean) {
                val value = remember { mutableStateOf(false) }
                if (!value.value && !condition) return
                val value2 = remember { mutableStateOf(false) }
                Text("Text ${'$'}{value.value}, ${'$'}{value2.value}")
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }
        """
    )

    @Test
    fun testVarargsIntrinsicRemember() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(vararg strings: String) {
                val show = remember { mutableStateOf(false) }
                if (show.value) {
                    Text("Showing")
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }
        """
    )

    @Test // regression test for b/267586102
    fun testRememberInALoop() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            val content: @Composable (a: SomeUnstableClass) -> Unit = {
                for (index in 0 until count) {
                    val i = remember { index }
                }
                val a = remember { 1 }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            val count = 0
            class SomeUnstableClass(val a: Any = "abc")
        """
    )

    @Test // Regression test for b/267586102 to ensure the fix doesn't insert unnecessary groups
    fun testRememberInALoop_NoTrailingRemember() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            val content: @Composable (a: SomeUnstableClass) -> Unit = {
                for (index in 0 until count) {
                    val i = remember { index }
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                val count = 0
                class SomeUnstableClass(val a: Any = "abc")
            """
    )

    @Test
    fun testRememberWithUnstableUnused_InInlineLambda() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String, unstable: List<*>) {
                InlineWrapper {
                    remember(param) { param }
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                @Composable inline fun InlineWrapper(block: @Composable () -> Unit) {}
            """,
    )

    @Test
    fun testRememberWithUnstable_InInlineLambda() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String, unstable: List<*>) {
                println(unstable)
                InlineWrapper {
                    remember(param) { param }
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                @Composable inline fun InlineWrapper(block: @Composable () -> Unit) {}
            """,
    )

    @Test
    fun testRememberWithUnstable_inLambda() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String, unstable: List<*>) {
                Wrapper {
                    remember(param, unstable) { param }
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                @Composable fun Wrapper(block: @Composable () -> Unit) {}
            """,
    )

    @Test
    fun testRememberExpressionMeta() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String) {
                val a = remember { param }
                Test(a)
            }
        """,
    )

    @Test
    fun testMemoizationWStableCapture() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String, unstable: List<*>) {
                Wrapper {
                    println(param)
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                @Composable fun Wrapper(block: () -> Unit) {}
            """,
    )

    @Test
    fun testMemoizationWUnstableCapture() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String, unstable: List<*>) {
                Wrapper {
                    println(unstable)
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                @Composable fun Wrapper(block: () -> Unit) {}
            """,
    )
}

class RememberIntrinsicTransformTestsStrongSkipping(
    useFir: Boolean
) : AbstractIrTransformTest(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, true)
        put(
            ComposeConfiguration.FEATURE_FLAGS,
            listOf(
                FeatureFlag.IntrinsicRemember.featureName,
                FeatureFlag.OptimizeNonSkippingGroups.featureName,
                FeatureFlag.StrongSkipping.featureName
            )
        )
    }

    @Test
    fun testMemoizationWStableCapture() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String, unstable: List<*>) {
                Wrapper {
                    println(param)
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                @Composable fun Wrapper(block: () -> Unit) {}
            """,
    )

    @Test
    fun testMemoizationWUnstableCapture() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String, unstable: List<*>) {
                Wrapper {
                    println(unstable)
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                @Composable fun Wrapper(block: () -> Unit) {}
            """,
    )

    @Test
    fun testRememberWithUnstableParam() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(param: String, unstable: List<*>) {
                remember(unstable) {
                    unstable[0]
                }
            }
        """,
    )

    @Test
    fun testRememberWithDefaultParams() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*

            val LocalColor = compositionLocalOf { 0 }
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable fun Icon(
                param: Int,
                defaultParam: Int = LocalColor.current
            ) {
                val remembered = remember(param, defaultParam) { TODO() }
            }
        """
    )

    @Test
    fun testRememberMethodReference() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable fun Icon(
                param: Int
            ) {
                val remembered = remember(param::toString) { TODO() }
            }
        """
    )
}
