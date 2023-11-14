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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.junit.Test

class LambdaMemoizationTransformTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, true)
        languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion = languageVersionSettings.languageVersion,
            apiVersion = languageVersionSettings.apiVersion,
            specificFeatures = mapOf(
                LanguageFeature.ContextReceivers to LanguageFeature.State.ENABLED
            )
        )
    }

    @Test
    fun testCapturedThisFromFieldInitializer() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            class A {
                val b = ""
                val c = @Composable {
                    print(b)
                }
            }
        """,
        """
        """
    )

    @Test
    fun testLocalInALocal() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable fun Example() {
                @Composable fun A() { }
                @Composable fun B(content: @Composable () -> Unit) { content() }
                @Composable fun C() {
                    B { A() }
                }
            }
        """,
        """
        """
    )

    // Fixes b/201252574
    @Test
    fun testLocalFunCaptures() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.Composable

            @NonRestartableComposable
            @Composable
            fun Err() {
                // `x` is not a capture of handler, but is treated as such.
                fun handler() {
                    { x: Int -> x }
                }
                // Lambda calling handler. To find captures, we need captures of `handler`.
                {
                  handler()
                }
            }
        """,
        """
        """
    )

    @Test
    fun testLocalClassCaptures1() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.Composable

            @NonRestartableComposable
            @Composable
            fun Err(y: Int, z: Int) {
                class Local {
                    val w = z
                    fun something(x: Int): Int { return x + y + w }
                }
                {
                  Local().something(2)
                }
            }
        """
    )

    @Test
    fun testLocalClassCaptures2() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable

            @NonRestartableComposable
            @Composable
            fun Example(z: Int) {
                class Foo(val x: Int) { val y = z }
                val lambda: () -> Any = {
                    Foo(1)
                }
            }
        """
    )

    @Test
    fun testLocalFunCaptures3() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun SimpleAnimatedContentSample() {
                @Composable fun Foo() {}

                AnimatedContent(1f) {
                    Foo()
                }
            }
        """,
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.animation.AnimatedVisibilityScope

            @Composable
            fun <S> AnimatedContent(
                targetState: S,
                content: @Composable AnimatedVisibilityScope.(targetState: S) -> Unit
            ) { }
        """
    )

    @Test
    fun testStateDelegateCapture() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.getValue

            @Composable fun A() {
                val x by mutableStateOf("abc")
                B {
                    print(x)
                }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable fun B(content: @Composable () -> Unit) {}
        """
    )

    @Test
    fun testTopLevelComposableLambdaProperties() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            val foo = @Composable {}
            val bar: @Composable () -> Unit = {}
        """,
        """
        """
    )

    @Test
    fun testLocalVariableComposableLambdas() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable fun A() {
                val foo = @Composable {}
                val bar: @Composable () -> Unit = {}
                B(foo)
                B(bar)
            }
        """,
        """
            import androidx.compose.runtime.Composable
            @Composable fun B(content: @Composable () -> Unit) {}
        """
    )

    @Test
    fun testParameterComposableLambdas() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable fun A() {
                B {}
            }
        """,
        """
            import androidx.compose.runtime.Composable
            @Composable fun B(content: @Composable () -> Unit) {}
        """
    )

    @Test // Regression test for b/180168881
    fun testFunctionReferenceWithinInferredComposableLambda() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            fun Problem() {
                fun foo() { }
                val lambda: @Composable ()->Unit = {
                    ::foo
                }
            }
        """
    )

    @Test
    fun testFunctionReferenceNonComposableMemoization() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            @Composable fun Example(x: Int) {
                fun foo() { use(x) }
                val shouldMemoize: ()->(()->Unit) = { ::foo }
            }
        """,
        """
            fun use(x: Any) = println(x)
        """
    )

    @Test // regression of b/162575428
    fun testComposableInAFunctionParameter() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test(enabled: Boolean, content: @Composable () -> Unit = {
                    Display("%enabled")
                }
            ) {
                Wrap(content)
            }
        """.replace('%', '$'),
        """
            import androidx.compose.runtime.Composable

            @Composable fun Display(text: String) { }
            @Composable fun Wrap(content: @Composable () -> Unit) { }
        """
    )

    @Test
    fun testComposabableLambdaInLocalDeclaration() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test(enabled: Boolean) {
                val content: @Composable () -> Unit = {
                    Display("%enabled")
                }
                Wrap(content)
            }
        """.replace('%', '$'),
        """
            import androidx.compose.runtime.Composable

            @Composable fun Display(text: String) { }
            @Composable fun Wrap(content: @Composable () -> Unit) { }
        """
    )

    // Ensure we don't remember lambdas that do not capture variables.
    @Test
    fun testLambdaNoCapture() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun TestLambda(content: () -> Unit) {
              content()
            }

            @Composable
            fun Test() {
              TestLambda {
                println("Doesn't capture")
              }
            }
        """
    )

    // Ensure the above test is valid as this should remember the lambda
    @Test
    fun testLambdaDoesCapture() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun TestLambda(content: () -> Unit) {
              content()
            }

            @Composable
            fun Test(a: String) {
              TestLambda {
                println("Captures a" + a)
              }
            }
        """
    )

    // We have to use composableLambdaInstance in crossinline lambdas, since they may be captured
    // in anonymous objects and called in a context with a different composer.
    @Test
    fun testCrossinlineLambda() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test() {
              var lambda: (@Composable () -> Unit)? = null
              f { s -> lambda = { Text(s) } }
              lambda?.let { it() }
            }
        """,
        """
            import androidx.compose.runtime.Composable
            @Composable fun Text(s: String) {}
            inline fun f(crossinline block: (String) -> Unit) = block("")
        """
    )

    // The lambda argument to remember and cache should not contain composable calls so
    // we have to use composableLambdaInstance.
    @Test
    fun testRememberComposableLambda() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Test(s: String) {
              remember<@Composable () -> Unit> { { Text(s) } }()
              currentComposer.cache<@Composable () -> Unit>(false) { { Text(s) } }()
            }
        """,
        """
            import androidx.compose.runtime.Composable
            @Composable fun Text(s: String) {}
        """
    )

    @Test
    fun memoizeLambdaInsideFunctionReturningValue() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test(foo: Foo): Int =
              Consume { foo.value }
        """,
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.Stable

            @Composable
            fun Consume(block: () -> Int): Int = block()

            @Stable
            class Foo {
                val value: Int = 0
            }
        """
    )

    @Test
    fun testComposableCaptureInDelegates() {
        verifyGoldenComposeIrTransform(
            """
                import androidx.compose.runtime.*

                class Test(val value: Int) : Delegate by Impl({
                    value
                })
            """,
            """
                import androidx.compose.runtime.Composable

                interface Delegate {
                    val content: @Composable () -> Unit
                }

                class Impl(override val content: @Composable () -> Unit) : Delegate
            """
        )
    }

    @Test
    fun testNonComposableFunctionReferenceWithStableExtensionReceiverMemoization() =
        verifyComposeIrTransform(
            extra = """
            class Stable
            fun Stable.foo() {}
        """,
            source = """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.remember

            @NonRestartableComposable
            @Composable
            fun Example() {
                val x = remember { Stable() }
                val shouldMemoize = x::foo
            }
        """,
            expectedTransformed = """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<rememb...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val x = remember({
                Stable()
              }, %composer, 0)
              val shouldMemoize = <block>{
                val tmp0 = x
                %composer.startReplaceableGroup(<>)
                val tmpCache = %composer.cache(%composer.changed(tmp0)) {
                  tmp0::foo
                }
                %composer.endReplaceableGroup()
                tmpCache
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
        )

    @Test
    fun testNonComposableFunctionReferenceWithUnstableExtensionReceiverMemoization() =
        verifyComposeIrTransform(
            extra = """
            class Unstable {
                var value: Int = 0
            }
            fun Unstable.foo() = {}
        """,
            source = """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.remember

            @NonRestartableComposable
            @Composable
            fun Example() {
                val x = remember { Unstable() }
                val shouldNotMemoize = x::foo
            }
        """,
            expectedTransformed = """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<rememb...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val x = remember({
                Unstable()
              }, %composer, 0)
              val shouldNotMemoize = x::foo
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """
        )

    @Test // Regression validating b/246399235
    fun testB246399235() {
        testCompile(
            """
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.clickable
            import androidx.compose.ui.composed
            import androidx.compose.foundation.interaction.MutableInteractionSource

            @Composable
            fun Something() {
                Modifier.noRippleClickable { }
            }

            inline fun Modifier.noRippleClickable(crossinline onClick: () -> Unit): Modifier {
                return composed {
                    clickable(MutableInteractionSource(), null, enabled = false) {
                        onClick()
                    }
                }
            }
            """
        )
    }

    @Test // Regression validating b/246399235 without function returning a value
    fun testB246399235_noReturn() {
        testCompile(
            """
            import androidx.compose.runtime.Composable

            @Composable
            fun Something() {
                noRippleClickable { }
            }

            inline fun noRippleClickable(crossinline onClick: () -> Unit) {
                 composed {
                    clickable {
                        onClick()
                    }
                 }
            }

            fun composed(block: @Composable () -> Unit) { }

            fun clickable(onClick: () -> Unit) { }
            """
        )
    }

    @Test
    fun testNonComposableFunctionReferenceWithNoArgumentsMemoization() {
        verifyComposeIrTransform(
            source = """
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.remember

                class Stable { fun qux() {} }

                @Composable
                fun Something() {
                    val x = remember { Stable() }
                    val shouldMemoize = x::qux
                }
            """,
            expectedTransformed = """
                @StabilityInferred(parameters = 1)
                class Stable {
                  fun qux() { }
                  static val %stable: Int = 0
                }
                @Composable
                fun Something(%composer: Composer?, %changed: Int) {
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(Something)<rememb...>:Test.kt")
                  if (%changed != 0 || !%composer.skipping) {
                    if (isTraceInProgress()) {
                      traceEventStart(<>, %changed, -1, <>)
                    }
                    val x = remember({
                      Stable()
                    }, %composer, 0)
                    val shouldMemoize = <block>{
                      val tmp0 = x
                      %composer.startReplaceableGroup(<>)
                      val tmpCache = %composer.cache(%composer.changed(tmp0)) {
                        tmp0::qux
                      }
                      %composer.endReplaceableGroup()
                      tmpCache
                    }
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Something(%composer, updateChangedFlags(%changed or 0b0001))
                  }
                }
            """
        )
    }

    // Validate fix for b/302680514.
    @Test
    fun testNonComposableFunctionReferenceWithArgumentsMemoization() {
        verifyComposeIrTransform(
            source = """
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.remember

                class Stable { fun qux(arg1: Any) {} }

                @Composable
                fun Something() {
                    val x = remember { Stable() }
                    val shouldMemoize = x::qux
                }
            """,
            expectedTransformed = """
                @StabilityInferred(parameters = 1)
                class Stable {
                  fun qux(arg1: Any) { }
                  static val %stable: Int = 0
                }
                @Composable
                fun Something(%composer: Composer?, %changed: Int) {
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(Something)<rememb...>:Test.kt")
                  if (%changed != 0 || !%composer.skipping) {
                    if (isTraceInProgress()) {
                      traceEventStart(<>, %changed, -1, <>)
                    }
                    val x = remember({
                      Stable()
                    }, %composer, 0)
                    val shouldMemoize = <block>{
                      val tmp0 = x
                      %composer.startReplaceableGroup(<>)
                      val tmpCache = %composer.cache(%composer.changed(tmp0)) {
                        tmp0::qux
                      }
                      %composer.endReplaceableGroup()
                      tmpCache
                    }
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Something(%composer, updateChangedFlags(%changed or 0b0001))
                  }
                }
            """
        )
    }

    // Reference to function with context receivers does not currently support memoization.
    @Test
    fun testNonComposableFunctionReferenceWithStableContextReceiverNotMemoized() {
        verifyComposeIrTransform(
            source = """
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.remember

                class StableReceiver
                class Stable {
                    context(StableReceiver)
                    fun qux() {}
                }

                @Composable
                fun Something() {
                    val x = remember { Stable() }
                    val shouldNotMemoize = x::qux
                }
            """,
            expectedTransformed = """
                @StabilityInferred(parameters = 1)
                class StableReceiver {
                  static val %stable: Int = 0
                }
                @StabilityInferred(parameters = 1)
                class Stable {
                  fun qux(%context_receiver_0: StableReceiver) { }
                  static val %stable: Int = 0
                }
                @Composable
                fun Something(%composer: Composer?, %changed: Int) {
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(Something)<rememb...>:Test.kt")
                  if (%changed != 0 || !%composer.skipping) {
                    if (isTraceInProgress()) {
                      traceEventStart(<>, %changed, -1, <>)
                    }
                    val x = remember({
                      Stable()
                    }, %composer, 0)
                    val shouldNotMemoize = x::qux
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Something(%composer, updateChangedFlags(%changed or 0b0001))
                  }
                }
            """
        )
    }

    @Test
    fun testUnstableReceiverFunctionReferenceNotMemoized() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Something() {
                val x = unstable::method
            }
        """,
        """
            class Unstable(var qux: Int = 0) { fun method(arg1: Int) {} }
            val unstable = Unstable()
        """
    )

    @Test
    fun testUnstableExtensionReceiverFunctionReferenceNotMemoized() =
        verifyGoldenComposeIrTransform(
            """
            import androidx.compose.runtime.Composable

            @Composable
            fun Something() {
                val x = unstable::method
            }
        """,
            """
            class Unstable(var foo: Int = 0)
            fun Unstable.method(arg1: Int) {}
            val unstable = Unstable()
        """
        )

    @Test
    fun testLocalFunctionReference() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Something(param: String) {
                fun method() {
                    println(param)
                }
                val x = ::method
            }
        """
    )

    @Test
    fun testLocalFunctionReferenceWReceiver() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Something(param: String, rcvr: Int) {
                fun Int.method() {
                    println(param)
                }
                val x = rcvr::method
            }
        """
    )
}
