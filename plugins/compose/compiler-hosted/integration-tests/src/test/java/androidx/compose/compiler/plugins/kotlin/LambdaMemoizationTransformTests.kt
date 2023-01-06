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

import org.junit.Test

class LambdaMemoizationTransformTests : AbstractIrTransformTest() {
    @Test
    fun testCapturedThisFromFieldInitializer() = verifyComposeIrTransform(
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
            @StabilityInferred(parameters = 0)
            class A {
              val b: String = ""
              val c: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  print(b)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              static val %stable: Int = 0
            }
        """,
        """
        """
    )

    @Test
    fun testLocalInALocal() = verifyComposeIrTransform(
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
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                @Composable
                fun A(%composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(A):Test.kt")
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endReplaceableGroup()
                }
                @Composable
                @ComposableInferredTarget(scheme = "[0[0]]")
                fun B(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(B)<conten...>:Test.kt")
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  content(%composer, 0b1110 and %changed)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endReplaceableGroup()
                }
                @Composable
                fun C(%composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(C)<B>:Test.kt")
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  B(composableLambda(%composer, <>, false) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<A()>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      if (isTraceInProgress()) {
                        traceEventStart(<>, %changed, -1, <>)
                      }
                      A(%composer, 0)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }, %composer, 0b0110)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endReplaceableGroup()
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        """
        """
    )

    // Fixes b/201252574
    @Test
    fun testLocalFunCaptures() = verifyComposeIrTransform(
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
            @NonRestartableComposable
            @Composable
            fun Err(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Err):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              fun handler() {
                { x: Int ->
                  x
                }
              }
              {
                handler()
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """,
        """
        """
    )

    @Test
    fun testLocalClassCaptures1() = verifyComposeIrTransform(
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
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Err(y: Int, z: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Err)<{>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              class Local {
                val w: Int = z
                fun something(x: Int): Int {
                  return x + y + w
                }
              }
              remember(y, z, {
                {
                  Local().something(2)
                }
              }, %composer, 0b1110 and %changed or 0b01110000 and %changed)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """,
        """
        """
    )

    @Test
    fun testLocalClassCaptures2() = verifyComposeIrTransform(
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
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(z: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<{>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              class Foo(val x: Int) {
                val y: Int = z
              }
              val lambda = remember(z, {
                {
                  Foo(1)
                }
              }, %composer, 0b1110 and %changed)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
        """,
        """
        """
    )

    @Test
    fun testLocalFunCaptures3() = verifyComposeIrTransform(
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
            @Composable
            fun SimpleAnimatedContentSample(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(SimpleAnimatedContentSample)<Animat...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                @Composable
                fun Foo(%composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(Foo):Test.kt")
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endReplaceableGroup()
                }
                AnimatedContent(1.0f, composableLambda(%composer, <>, false) { it: Float, %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<Foo()>:Test.kt")
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  Foo(%composer, 0)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                }, %composer, 0b00110110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                SimpleAnimatedContentSample(%composer, updateChangedFlags(%changed or 0b0001))
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
    fun testStateDelegateCapture() = verifyComposeIrTransform(
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
            @Composable
            fun A(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<B>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val x by {
                  val x%delegate = mutableStateOf(
                    value = "abc"
                  )
                  get() {
                    return x%delegate.getValue(null, ::x%delegate)
                  }
                }
                B(composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C:Test.kt")
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    if (isTraceInProgress()) {
                      traceEventStart(<>, %changed, -1, <>)
                    }
                    print(<get-x>())
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable fun B(content: @Composable () -> Unit) {}
        """
    )

    @Test
    fun testTopLevelComposableLambdaProperties() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            val foo = @Composable {}
            val bar: @Composable () -> Unit = {}
        """,
        """
            val foo: Function2<Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
            val bar: Function2<Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-2
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  Unit
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              val lambda-2: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  Unit
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """,
        """
        """
    )

    @Test
    fun testLocalVariableComposableLambdas() = verifyComposeIrTransform(
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
            @Composable
            fun A(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<B(foo)>,<B(bar)>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val foo = ComposableSingletons%TestKt.lambda-1
                val bar = ComposableSingletons%TestKt.lambda-2
                B(foo, %composer, 0b0110)
                B(bar, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  Unit
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              val lambda-2: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  Unit
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable
            @Composable fun B(content: @Composable () -> Unit) {}
        """
    )

    @Test
    fun testParameterComposableLambdas() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable fun A() {
                B {}
            }
        """,
        """
            @Composable
            fun A(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<B>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                B(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  Unit
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable
            @Composable fun B(content: @Composable () -> Unit) {}
        """
    )

    @Test // Regression test for b/180168881
    fun testFunctionReferenceWithinInferredComposableLambda() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            fun Problem() {
                fun foo() { }
                val lambda: @Composable ()->Unit = {
                    ::foo
                }
            }
        """,
        """
            fun Problem() {
              fun foo() { }
              val lambda = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  foo
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testFunctionReferenceNonComposableMemoization() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            @Composable fun Example(x: Int) {
                fun foo() { use(x) }
                val shouldMemoize: ()->(()->Unit) = { ::foo }
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<{>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                fun foo() {
                  use(x)
                }
                val shouldMemoize = remember(x, {
                  {
                    foo
                  }
                }, %composer, 0b1110 and %dirty)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(x, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        """
            fun use(x: Any) = println(x)
        """.trimIndent()
    )

    @Test // regression of b/162575428
    fun testComposableInAFunctionParameter() = verifyComposeIrTransform(
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
            @Composable
            fun Test(enabled: Boolean, content: Function2<Composer, Int, Unit>?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(1)<Wrap(c...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(enabled)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(content)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (%default and 0b0010 !== 0) {
                  content = composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<Displa...>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      if (isTraceInProgress()) {
                        traceEventStart(<>, %changed, -1, <>)
                      }
                      Display("%enabled", %composer, 0)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                }
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Wrap(content, %composer, 0b1110 and %dirty shr 0b0011)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(enabled, content, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable fun Display(text: String) { }
            @Composable fun Wrap(content: @Composable () -> Unit) { }
        """
    )

    @Test
    fun testComposabableLambdaInLocalDeclaration() = verifyComposeIrTransform(
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
            @Composable
            fun Test(enabled: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Wrap(c...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(enabled)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val content = composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<Displa...>:Test.kt")
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    if (isTraceInProgress()) {
                      traceEventStart(<>, %changed, -1, <>)
                    }
                    Display("%enabled", %composer, 0)
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }
                Wrap(content, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(enabled, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable fun Display(text: String) { }
            @Composable fun Wrap(content: @Composable () -> Unit) { }
        """
    )

    // Ensure we don't remember lambdas that do not capture variables.
    @Test
    fun testLambdaNoCapture() = verifyComposeIrTransform(
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
        """,
        """
            @Composable
            fun TestLambda(content: Function0<Unit>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(TestLambda):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(content)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                content()
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                TestLambda(content, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<TestLa...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                TestLambda({
                  println("Doesn't capture")
                }, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
    """
    )

    // Ensure the above test is valid as this should remember the lambda
    @Test
    fun testLambdaDoesCapture() = verifyComposeIrTransform(
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
        """,
        """
        @Composable
        fun TestLambda(content: Function0<Unit>, %composer: Composer?, %changed: Int) {
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(TestLambda):Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changedInstance(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            if (isTraceInProgress()) {
              traceEventStart(<>, %changed, -1, <>)
            }
            content()
            if (isTraceInProgress()) {
              traceEventEnd()
            }
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            TestLambda(content, %composer, updateChangedFlags(%changed or 0b0001))
          }
        }
        @Composable
        fun Test(a: String, %composer: Composer?, %changed: Int) {
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<{>,<TestLa...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            if (isTraceInProgress()) {
              traceEventStart(<>, %dirty, -1, <>)
            }
            TestLambda(remember(a, {
              {
                println("Captures a" + a)
              }
            }, %composer, 0b1110 and %dirty), %composer, 0)
            if (isTraceInProgress()) {
              traceEventEnd()
            }
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(a, %composer, updateChangedFlags(%changed or 0b0001))
          }
        }
        """
    )

    // We have to use composableLambdaInstance in crossinline lambdas, since they may be captured
    // in anonymous objects and called in a context with a different composer.
    @Test
    fun testCrossinlineLambda() = verifyComposeIrTransform(
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
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<it()>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                var lambda = null
                f { s: String ->
                  lambda = composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<Text(s...>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      if (isTraceInProgress()) {
                        traceEventStart(<>, %changed, -1, <>)
                      }
                      Text(s, %composer, 0)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                }
                val tmp0_safe_receiver = lambda
                val tmp0_group = when {
                  tmp0_safe_receiver == null -> {
                    null
                  }
                  else -> {
                    tmp0_safe_receiver.let { it: Function2<Composer, Int, Unit> ->
                      it(%composer, 0)
                    }
                  }
                }
                tmp0_group
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
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
    fun testRememberComposableLambda() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Test(s: String) {
              remember<@Composable () -> Unit> { { Text(s) } }()
              currentComposer.cache<@Composable () -> Unit>(false) { { Text(s) } }()
            }
        """,
        """
            @Composable
            fun Test(s: String, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<rememb...>,<rememb...>,<curren...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(s)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                remember({
                  composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<Text(s...>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      if (isTraceInProgress()) {
                        traceEventStart(<>, %changed, -1, <>)
                      }
                      Text(s, %composer, 0b1110 and %dirty)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                }, %composer, 0)(%composer, 6)
                %composer.cache(false) {
                  composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<Text(s...>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      if (isTraceInProgress()) {
                        traceEventStart(<>, %changed, -1, <>)
                      }
                      Text(s, %composer, 0b1110 and %dirty)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                }
                (%composer, 6)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(s, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable
            @Composable fun Text(s: String) {}
        """
    )

    @Test
    fun memoizeLambdaInsideFunctionReturningValue() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test(foo: Foo): Int =
              Consume { foo.value }
        """,
        """
            @Composable
            fun Test(foo: Foo, %composer: Composer?, %changed: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)<{>,<Consum...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0 = Consume(remember(foo, {
                {
                  foo.value
                }
              }, %composer, 0b1110 and %changed), %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }

        """.trimIndent(),
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.Stable

            @Composable
            fun Consume(block: () -> Int): Int = block()

            @Stable
            class Foo {
                val value: Int = 0
            }
        """.trimIndent()
    )

    @Test
    fun testComposableCaptureInDelegates() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            class Test(val value: Int) : Delegate by Impl({
                value
            })
        """,
        """
            @StabilityInferred(parameters = 0)
            class Test(val value: Int) : Delegate {
              private val %%delegate_0: Impl = Impl(composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  value
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              )
              val content: Function2<Composer, Int, Unit>
                get() {
                  return <this>.%%delegate_0.content
                }
              static val %stable: Int = 0
            }
        """,
        """
            import androidx.compose.runtime.Composable

            interface Delegate {
                val content: @Composable () -> Unit
            }

            class Impl(override val content: @Composable () -> Unit) : Delegate
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
}
