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
import org.junit.Test

class ControlFlowTransformTestsNoSource(
    useFir: Boolean
) : AbstractControlFlowTransformTests(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, false)
        put(
            ComposeConfiguration.FEATURE_FLAGS,
            listOf(FeatureFlag.OptimizeNonSkippingGroups.featureName)
        )
        put(ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY, false)
    }

    @Test
    fun testPublicFunctionAlwaysMarkedAsCall(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              A(a)
              A(b)
            }
        """
    )

    @Test
    fun testPrivateFunctionDoNotGetMarkedAsCall(): Unit = controlFlow(
        """
            @Composable
            private fun Test() {
              A(a)
              A(b)
            }
        """
    )

    @Test
    fun testCallingAWrapperComposable(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              W {
                A()
              }
            }
        """
    )

    @Test
    fun testCallingAnInlineWrapperComposable(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              IW {
                A()
              }
            }
        """
    )

    @Test
    fun verifyEarlyExitFromMultiLevelNestedInlineFunction() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            @NonRestartableComposable
            fun Test(condition: Boolean) {
                Text("Before outer")
                InlineLinearA outer@{
                    Text("Before inner")
                    InlineLinearB {
                        Text("Before return")
                        if (condition) return@outer
                        Text("After return")
                    }
                    Text("After inner")
                }
                Text("Before outer")
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }

            @Composable
            inline fun InlineLinearA(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            inline fun InlineLinearB(content: @Composable () -> Unit) {
                content()
            }
        """
    )
}
