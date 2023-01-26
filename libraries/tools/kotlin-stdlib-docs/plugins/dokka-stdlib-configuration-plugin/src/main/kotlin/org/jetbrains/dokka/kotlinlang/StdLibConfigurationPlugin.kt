/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.analysis.DokkaAnalysisConfiguration
import org.jetbrains.dokka.analysis.ProjectKotlinAnalysis
import org.jetbrains.dokka.plugability.configuration

class StdLibConfigurationPlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    @Suppress("unused")
    val stdLibKotlinAnalysis by extending {
        dokkaBase.kotlinAnalysis providing { ctx ->
            val ignoreCommonBuiltIns = configuration<StdLibConfigurationPlugin, StdLibAnalysisConfiguration>(ctx)?.ignoreCommonBuiltIns ?: false
            ProjectKotlinAnalysis(
                sourceSets = ctx.configuration.sourceSets,
                logger = ctx.logger,
                analysisConfiguration = DokkaAnalysisConfiguration(ignoreCommonBuiltIns = ignoreCommonBuiltIns)
            )
        } override dokkaBase.defaultKotlinAnalysis
    }
}