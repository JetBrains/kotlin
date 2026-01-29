/*
 * Copyright 2024 The Android Open Source Project
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
import org.junit.runners.Parameterized
import kotlin.test.Test

class ComposePausableCompositionTests(
    useFir: Boolean,
    private val pausableEnabled: Boolean
) : AbstractControlFlowTransformTests(useFir) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useFir = {0}, pausableEnabled = {1}")
        fun data() = if (isCI()) {
            arrayOf<Any>(
                arrayOf(true, false),
                arrayOf(true, true)
            )
        } else {
            arrayOf<Any>(
                arrayOf(false, false),
                arrayOf(false, true),
                arrayOf(true, false),
                arrayOf(true, true)
            )
        }
    }

    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, false)
        put(ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY, false)
        put(
            ComposeConfiguration.FEATURE_FLAGS,
            listOf(FeatureFlag.PausableComposition.name(pausableEnabled))
        )
    }

    @Test
    fun testRestartableComposableFunction() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(a: Int, b: String, c: Float) {
                use(a)
                use(b)
                use(c)
            }
        """,
        extra = """
            fun use(value: Any?) { println(value) } 
        """
    )

    @Test
    fun testRestartableComposableLambda() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(a: Int, b: String, c: Float) {
                Wrap {
                    use(a)
                    use(b)
                    use(c)
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            fun use(value: Any?) { println(value) }
            @Composable fun Wrap(content: @Composable () -> Unit) = content()
        """
    )

}