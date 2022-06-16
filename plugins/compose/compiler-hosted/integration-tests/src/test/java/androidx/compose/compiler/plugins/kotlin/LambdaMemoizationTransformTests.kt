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

class LambdaMemoizationTransformTests : ComposeIrTransformTest() {
    @Test
    fun testCapturedThisFromFieldInitializer(): Unit = verifyComposeIrTransform(
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
                  print(b)
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
    fun testLocalInALocal(): Unit = verifyComposeIrTransform(
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
                @Composable
                fun A(%composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(A):Test.kt")
                  %composer.endReplaceableGroup()
                }
                @Composable
                @ComposableInferredTarget(scheme = "[0[0]]")
                fun B(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(B)<conten...>:Test.kt")
                  content(%composer, 0b1110 and %changed)
                  %composer.endReplaceableGroup()
                }
                @Composable
                fun C(%composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(C)<B>:Test.kt")
                  B(composableLambda(%composer, <>, false) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<A()>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      A(%composer, 0)
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }, %composer, 0b0110)
                  %composer.endReplaceableGroup()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(%composer, %changed or 0b0001)
              }
            }
        """,
        """
        """
    )

    // Fixes b/201252574
    @Test
    fun testLocalFunCaptures(): Unit = verifyComposeIrTransform(
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
              fun handler() {
                { x: Int ->
                  x
                }
              }
              {
                handler()
              }
              %composer.endReplaceableGroup()
            }
        """,
        """
        """
    )

    @Test
    fun testLocalClassCaptures1(): Unit = verifyComposeIrTransform(
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
              %composer.endReplaceableGroup()
            }
        """,
        """
        """
    )

    @Test
    fun testLocalClassCaptures2(): Unit = verifyComposeIrTransform(
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
              class Foo(val x: Int) {
                val y: Int = z
              }
              val lambda = remember(z, {
                {
                  Foo(1)
                }
              }, %composer, 0b1110 and %changed)
              %composer.endReplaceableGroup()
            }
        """,
        """
        """
    )

    @Test
    fun testLocalFunCaptures3(): Unit = verifyComposeIrTransform(
        """
            import androidx.compose.animation.AnimatedContent
            import androidx.compose.animation.ExperimentalAnimationApi
            import androidx.compose.runtime.Composable

            @OptIn(ExperimentalAnimationApi::class)
            @Composable
            fun SimpleAnimatedContentSample() {
                @Composable fun Foo() {}

                AnimatedContent(1f) {
                    Foo()
                }
            }
        """,
        """
            @OptIn(markerClass = ExperimentalAnimationApi::class)
            @Composable
            fun SimpleAnimatedContentSample(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(SimpleAnimatedContentSample)<Animat...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                @Composable
                fun Foo(%composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(Foo):Test.kt")
                  %composer.endReplaceableGroup()
                }
                AnimatedContent(1.0f, null, null, null, composableLambda(%composer, <>, false) { it: Float, %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<Foo()>:Test.kt")
                  Foo(%composer, 0)
                }, %composer, 0b0110000000000110, 0b1110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                SimpleAnimatedContentSample(%composer, %changed or 0b0001)
              }
            }
        """,
        """
        """
    )

    @Test
    fun testStateDelegateCapture(): Unit = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.getValue

            @Composable fun A() {
                val x by mutableStateOf(123)
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
                <<LOCALDELPROP>>
                B(composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C:Test.kt")
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    print(<get-x>())
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%composer, %changed or 0b0001)
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable fun B(content: @Composable () -> Unit) {}
        """
    )

    @Test
    fun testTopLevelComposableLambdaProperties(): Unit = verifyComposeIrTransform(
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
                  Unit
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              val lambda-2: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  Unit
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
    fun testLocalVariableComposableLambdas(): Unit = verifyComposeIrTransform(
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
                val foo = ComposableSingletons%TestKt.lambda-1
                val bar = ComposableSingletons%TestKt.lambda-2
                B(foo, %composer, 0b0110)
                B(bar, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%composer, %changed or 0b0001)
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  Unit
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              val lambda-2: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  Unit
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
    fun testParameterComposableLambdas(): Unit = verifyComposeIrTransform(
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
                B(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%composer, %changed or 0b0001)
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  Unit
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

    @Test // regression of b/162575428
    fun testComposableInAFunctionParameter(): Unit = verifyComposeIrTransform(
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
                %dirty = %dirty or if (%composer.changed(content)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (%default and 0b0010 !== 0) {
                  content = composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<Displa...>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      Display("%enabled", %composer, 0)
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                }
                Wrap(content, %composer, 0b1110 and %dirty shr 0b0011)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(enabled, content, %composer, %changed or 0b0001, %default)
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
    fun testComposabableLambdaInLocalDeclaration(): Unit = verifyComposeIrTransform(
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
                val content = composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<Displa...>:Test.kt")
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    Display("%enabled", %composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }
                Wrap(content, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(enabled, %composer, %changed or 0b0001)
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
                %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                content()
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                TestLambda(content, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<TestLa...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                TestLambda({
                  println("Doesn't capture")
                }, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
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
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            content()
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            TestLambda(content, %composer, %changed or 0b0001)
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
            TestLambda(remember(a, {
              {
                println("Captures a" + a)
              }
            }, %composer, 0b1110 and %dirty), %composer, 0)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(a, %composer, %changed or 0b0001)
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
                var lambda = null
                f { s: String ->
                  lambda = composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<Text(s...>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      Text(s, %composer, 0)
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
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
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
                remember({
                  composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<Text(s...>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      Text(s, %composer, 0b1110 and %dirty)
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                }, %composer, 0)(%composer, 6)
                %composer.cache(false) {
                  composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                    sourceInformation(%composer, "C<Text(s...>:Test.kt")
                    if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                      Text(s, %composer, 0b1110 and %dirty)
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                }
                (%composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(s, %composer, %changed or 0b0001)
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable
            @Composable fun Text(s: String) {}
        """
    )
}
