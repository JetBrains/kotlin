/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider

internal class ToolingDiagnosticRenderingOptions(
    val isVerbose: Boolean,
    val suppressedWarningIds: List<String>,
    val suppressedErrorIds: List<String>
) {
    companion object {
        fun forProject(project: Project): ToolingDiagnosticRenderingOptions = with(project.kotlinPropertiesProvider) {
            ToolingDiagnosticRenderingOptions(
                internalVerboseDiagnostics,
                suppressedGradlePluginWarnings,
                suppressedGradlePluginErrors
            )
        }
    }
}

internal fun Collection<ToolingDiagnostic>.withoutSuppressed(options: ToolingDiagnosticRenderingOptions): Collection<ToolingDiagnostic> =
    filter { !it.isSuppressed(options) }

internal fun ToolingDiagnostic.isSuppressed(options: ToolingDiagnosticRenderingOptions): Boolean =
    severity == ToolingDiagnostic.Severity.WARNING && id in options.suppressedWarningIds
            || severity == ToolingDiagnostic.Severity.ERROR && id in options.suppressedErrorIds
