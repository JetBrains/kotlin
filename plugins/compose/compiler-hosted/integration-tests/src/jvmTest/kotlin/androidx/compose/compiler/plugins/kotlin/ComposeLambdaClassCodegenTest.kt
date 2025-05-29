/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmClosureGenerationScheme
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.Test

@RunWith(JUnit4::class)
class ComposeLambdaClassCodegenTest : AbstractIrTransformTest(useFir = true) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(JVMConfigurationKeys.LAMBDAS, JvmClosureGenerationScheme.CLASS)
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, true)
    }

    @Test
    fun testLambdaClassParameterInfo() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test(z: Int, x: Int, y: Result<Int>) {
                Wrapper { Test(z, x, y) }
            }

            @Composable
            fun Wrapper(content: @Composable () -> Unit) { content() }
        """
    )
}