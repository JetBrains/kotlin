/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle

internal object ComposePluginSuggestApplyChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        project.kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseDsl)

        // KMP project without Jetbrains Compose plugin
        if (project.plugins.hasKotlinMultiplatformPlugin() &&
            !project.plugins.hasJetBrainsComposePlugin() &&
            project.isAgpComposeEnabled &&
            !project.plugins.hasKotlinComposePlugin()
        ) {
            collector.reportSuggestion(diagnosticsContext)
        }
    }

    private fun KotlinToolingDiagnosticsCollector.reportSuggestion(
        context: ToolingDiagnosticsContext,
    ) {
        reportOncePerGradleProject(
            context,
            KotlinToolingDiagnostics.NoComposeCompilerPluginAppliedWarning(),
        )
    }

    private fun PluginContainer.hasKotlinComposePlugin() =
        hasPlugin("org.jetbrains.kotlin.plugin.compose")

    private fun PluginContainer.hasKotlinMultiplatformPlugin() =
        hasPlugin("kotlin-multiplatform") || hasPlugin("org.jetbrains.kotlin.multiplatform")

    private fun PluginContainer.hasJetBrainsComposePlugin() = hasPlugin("org.jetbrains.compose")

    private val Project.agpComposeConfiguration get() = configurations.findByName("kotlin-extension")

    private val Project.isAgpComposeEnabled get() = agpComposeConfiguration != null
}
