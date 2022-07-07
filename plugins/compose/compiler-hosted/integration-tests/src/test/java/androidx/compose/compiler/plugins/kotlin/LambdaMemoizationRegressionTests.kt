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

import org.junit.Test

/**
 * This test merely ensures that code gen changes are evaluated against potentially
 * breaking Android Studio compose debugger integration, see change id
 * I63ce10791fc3795a568f5f09ca6a24e801f5e3da
 *
 * The Android Studio debugger searches for `ComposableSingletons` classes by name.
 * Any changes to the naming scheme have to be reflected in the Android Studio code.
 */
class LambdaMemoizationRegressionTests : ComposeIrTransformTest() {
    @Test
    fun testNestedComposableSingletonsClass() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            class A {
                val x = @Composable {}
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            class A {
              val x: Function2<Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
              static val %stable: Int = 0
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
        """
    )

    @Test
    fun testNestedComposableSingletonsClass2() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            class A {
                class B {
                    val x = @Composable {}
                }
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            class A {
              @StabilityInferred(parameters = 0)
              class B {
                val x: Function2<Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
                static val %stable: Int = 0
              }
              static val %stable: Int = 0
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
        """
    )

    @Test
    fun testJvmNameComposableSingletons() = verifyComposeIrTransform(
        """
            @file:JvmName("A")
            import androidx.compose.runtime.Composable

            val x = @Composable {}
        """,
        """
            val x: Function2<Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
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
        """
    )
}