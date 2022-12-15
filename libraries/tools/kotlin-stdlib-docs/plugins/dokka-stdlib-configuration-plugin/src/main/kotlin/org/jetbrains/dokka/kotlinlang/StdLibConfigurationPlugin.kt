package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.analysis.DokkaAnalysisConfiguration
import org.jetbrains.dokka.analysis.KotlinAnalysis

class StdLibConfigurationPlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    @Suppress("unused")
    val stdLibKotlinAnalysis by extending {
        dokkaBase.kotlinAnalysis providing { ctx ->
            KotlinAnalysis(
                sourceSets = ctx.configuration.sourceSets,
                logger = ctx.logger,
                analysisConfiguration = DokkaAnalysisConfiguration(ignoreCommonBuiltIns = true)
            )
        } override dokkaBase.defaultKotlinAnalysis
    }
}