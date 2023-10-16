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
class TraceInformationTest(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    @Test
    fun testBasicComposableFunctions() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            class A {
              @Composable fun B(x: Int) { }
            }

            @Composable
            fun C() { A().B(1337) }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.TRUNCATE_KEY
    )

    @Test
    fun testReadOnlyComposable() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            @ReadOnlyComposable
            internal fun someFun(a: Boolean): Boolean {
                if (a) {
                    return a
                } else {
                    return a
                }
            }
        """
    )

    @Test
    fun testInlineFunctionsDonotGenerateTraceMarkers() = verifyGoldenComposeIrTransform(
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
            import androidx.compose.runtime.*

            @Composable
            fun A() { }
        """
    )
}
