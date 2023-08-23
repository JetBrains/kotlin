/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

object FormalVerificationPluginErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("FormalVerification").apply {
        put(
            PluginErrors.VIPER_TEXT,
            "Generated Viper text for {0}:\n{1}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        put(
            PluginErrors.VIPER_CONSISTENCY_ERROR,
            "Viper consistency error: {0}",
            CommonRenderers.STRING,
        )
        put(
            PluginErrors.VIPER_VERIFICATION_ERROR,
            "Viper verification error: {0}",
            CommonRenderers.STRING,
        )
        put(
            PluginErrors.FUNCTION_WITH_UNVERIFIED_CONTRACT,
            "Function {0} may not satisfy its contract.",
            CommonRenderers.STRING,
        )
    }
}