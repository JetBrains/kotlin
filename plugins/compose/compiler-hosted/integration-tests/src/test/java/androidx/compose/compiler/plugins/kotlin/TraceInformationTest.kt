/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.compiler.plugins.kotlin.AbstractIrTransformTest.TruncateTracingInfoMode
import org.junit.Test

/**
 * Verifies trace data passed to tracing. Relies on [TruncateTracingInfoMode.KEEP_INFO_STRING] to
 * leave most of the trace information in the test output.
 *
 * More complex cases tested in other IrTransform tests that use
 * the [TruncateTracingInfoMode.KEEP_INFO_STRING].
 */
class TraceInformationTest : AbstractIrTransformTest() {
    @Test
    fun testBasicComposableFunctions() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            class A {
              @Composable fun B(x: Int) { }
            }

            @Composable
            fun C() { A().B(1337) }
        """,
        """
            @StabilityInferred(parameters = 0)
            class A {
              @Composable
              fun B(x: Int, %composer: Composer?, %changed: Int) {
                %composer = %composer.startRestartGroup(<>)
                sourceInformation(%composer, "C(B):Test.kt")
                if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %changed, -1, <>)
                  }
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
                val tmp0_rcvr = <this>
                %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                  tmp0_rcvr.B(x, %composer, updateChangedFlags(%changed or 0b0001))
                }
              }
              static val %stable: Int = 0
            }
            @Composable
            fun C(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(C)<B(1337...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                A().B(1337, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                C(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.TRUNCATE_KEY
    )

    @Test
    fun testInlineFunctionsDonotGenerateTraceMarkers() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            inline fun Wrapper(content: @Composable () -> Unit) = content()

            @Composable
            fun Test(condition: Boolean) {
                A()
                Wrapper {
                    A()
                    if (!condition) return
                    A()
                }
                A()
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Wrapper(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "CC(Wrapper)<conten...>:Test.kt")
              content(%composer, 0b1110 and %changed)
              %composer.endReplaceableGroup()
            }
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>,<Wrappe...>,<A()>:Test.kt")
              val tmp0_marker = %composer.currentMarker
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                A(%composer, 0)
                Wrapper({ %composer: Composer?, %changed: Int ->
                  sourceInformationMarkerStart(%composer, <>, "C<A()>,<A()>:Test.kt")
                  A(%composer, 0)
                  if (!condition) {
                    %composer.endToMarker(tmp0_marker)
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                    %composer@Test.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                    return
                  }
                  A(%composer, 0)
                  sourceInformationMarkerEnd(%composer)
                }, %composer, 0)
                A(%composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        """
            import androidx.compose.runtime.*

            @Composable
            fun A() { }
        """
    )
}
