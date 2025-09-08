/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.Test

@RunWith(JUnit4::class)
class ComposeRuntimeTargetTests : AbstractIrTransformTest(useFir = true) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(
            ComposeConfiguration.TARGET_RUNTIME_VERSION_KEY,
            ComposeRuntimeVersion.v1_8.value
        )
        put(
            ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY,
            true
        )
    }

    @Test
    fun sourceParameters() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable fun Content(o2: Any, o1: Any, o5: Any, o3: Any) {}
        """
    )
}