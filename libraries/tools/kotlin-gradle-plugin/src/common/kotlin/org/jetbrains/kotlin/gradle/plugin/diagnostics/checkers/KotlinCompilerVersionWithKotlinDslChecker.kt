/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

internal object KotlinCompilerVersionWithKotlinDslChecker : KotlinGradleProjectChecker {
    private const val KOTLIN_DSL_PLUGIN_ID = "org.gradle.kotlin.kotlin-dsl"

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        // Wait until the build script is evaluated, so the plugin and the compiler version are seen wherever they are configured
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()
        if (!project.plugins.hasPlugin(KOTLIN_DSL_PLUGIN_ID)) return

        val compilerVersion = project.kotlinExtensionOrNull?.compilerVersion?.orNull ?: return
        val pluginVersion = project.getKotlinPluginVersion()
        if (compilerVersion == pluginVersion) return

        collector.report(
            diagnosticsContext,
            KotlinToolingDiagnostics.KotlinCompilerVersionWithKotlinDslPlugin(compilerVersion, pluginVersion),
        )
    }
}
