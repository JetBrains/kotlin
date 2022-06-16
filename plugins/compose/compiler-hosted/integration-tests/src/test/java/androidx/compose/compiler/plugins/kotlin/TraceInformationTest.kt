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
class TraceInformationTest : ComposeIrTransformTest() {
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
                } else {
                  %composer.skipToGroupEnd()
                }
                val tmp0_rcvr = <this>
                %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                  tmp0_rcvr.B(x, %composer, %changed or 0b0001)
                }
              }
              static val %stable: Int = 0
            }
            @Composable
            fun C(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(C)<B(1337...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                A().B(1337, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                C(%composer, %changed or 0b0001)
              }
            }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.TRUNCATE_KEY
    )
}
