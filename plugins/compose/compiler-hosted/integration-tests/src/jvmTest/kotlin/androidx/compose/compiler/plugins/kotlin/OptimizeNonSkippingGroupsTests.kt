/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.runners.Parameterized
import kotlin.test.Test

class OptimizeNonSkippingGroupsTests(
    private val optimizeNonSkippingGroups: Boolean
) : AbstractControlFlowTransformTests(useFir = true) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, true)
        put(ComposeConfiguration.FEATURE_FLAGS, listOf(FeatureFlag.OptimizeNonSkippingGroups.name(optimizeNonSkippingGroups)))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "optimize = {0}")
        fun data() = arrayOf<Any>(false, true)
    }

    // Regression test for b/405541364
    @Test
    fun testConditionalLaunchedEffectCall() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun TwoLambdas(
                lambda1: () -> Unit,
                lambda2: (Int) -> Unit
            ) {
                lambda1()
                lambda2(0)
            }
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(states: List<String>, condition: Boolean) {
                states.forEach { state ->
                    key(state) {
                        if (condition) {
                            LaunchedEffect(state) { println(state) }
                        }
                        TwoLambdas(
                            lambda1 = { println(state) },
                            lambda2 = { println(state) }
                        )
                    }
                }
            }
        """,
    )
}