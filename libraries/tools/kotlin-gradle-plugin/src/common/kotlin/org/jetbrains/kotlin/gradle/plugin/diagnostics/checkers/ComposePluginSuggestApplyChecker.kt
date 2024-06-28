/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle

internal object ComposePluginSuggestApplyChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        project.kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseDsl)

        // 'kotlin-android' project with AGP <8.5.0 where Compose enabled, but no Kotlin Compose Gradle plugin is used
        if (project.plugins.hasKotlinAndroidPlugin() &&
            !isAgp850AndAbove &&
            project.isAgpComposeEnabled &&
            !project.plugins.hasKotlinComposePlugin()
        ) {
            collector.reportSuggestion(project)
        } else if (
        // KMP project without Jetbrains Compose plugin, but with AGP <8.5.0 where Compose enabled,
        // and no Kotlin Compose Gradle plugin is used
            project.plugins.hasKotlinMultiplatformPlugin() &&
            !project.plugins.hasJetBrainsComposePlugin() &&
            project.isAgpComposeEnabled &&
            !isAgp850AndAbove &&
            !project.plugins.hasKotlinComposePlugin()
        ) {
            collector.reportSuggestion(project)
        }
    }

    private fun KotlinToolingDiagnosticsCollector.reportSuggestion(
        project: Project
    ) {
        reportOncePerGradleProject(
            project,
            KotlinToolingDiagnostics.NoComposeCompilerPluginAppliedWarning(),
        )
    }

    private fun PluginContainer.hasKotlinAndroidPlugin() =
        hasPlugin("kotlin-android") || hasPlugin("org.jetbrains.kotlin.android")

    private fun PluginContainer.hasKotlinComposePlugin() =
        hasPlugin("org.jetbrains.kotlin.plugin.compose")

    private fun PluginContainer.hasKotlinMultiplatformPlugin() =
        hasPlugin("kotlin-multiplatform") || hasPlugin("org.jetbrains.kotlin.multiplatform")

    private fun PluginContainer.hasJetBrainsComposePlugin() = hasPlugin("org.jetbrains.compose")

    private val Project.agpComposeConfiguration get() = configurations.findByName("kotlin-extension")

    private val Project.isAgpComposeEnabled get() = agpComposeConfiguration != null

    private val isAgp850AndAbove get() = AndroidGradlePluginVersion.current >= AndroidGradlePluginVersion(8, 5, 0)
}