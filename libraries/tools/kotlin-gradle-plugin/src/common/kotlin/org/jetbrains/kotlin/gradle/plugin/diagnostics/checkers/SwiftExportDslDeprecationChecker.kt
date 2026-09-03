/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.findExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportDSLConstants
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.export.EXPORT_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension

/**
 * Reports the deprecation of the legacy `swiftExport { }` DSL, and rejects a project that configures both
 * it and the `export { swift { } }` DSL.
 *
 * The two DSLs activate the Xcode integration differently: the legacy one turns it on just by being used,
 * the new one needs an explicit `xcodeIntegration()` call. Applying only one DSL and silently dropping the
 * other's configuration would be worse than failing the build, so the conflict is reported as an ERROR: that
 * fails every build that compiles Kotlin, which any build that produces Swift Export output does.
 *
 * Awaits [AfterFinaliseDsl] so that the order of the DSL calls in the build script doesn't matter.
 */
internal object SwiftExportDslDeprecationChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        AfterFinaliseDsl.await()

        val mppExtension = multiplatformExtension ?: return
        val legacyExtension = mppExtension.findExtension<SwiftExportExtension>(
            SwiftExportDSLConstants.SWIFT_EXPORT_EXTENSION_NAME
        ) ?: return

        val legacyDslUsed = mppExtension.isSwiftExportRequested || legacyExtension.isConfigured
        if (!legacyDslUsed) return

        val exportDslUsed = mppExtension
            .findExtension<ExportExtension>(EXPORT_EXTENSION_NAME)
            ?.isSwiftExportConfigured == true

        if (exportDslUsed) {
            collector.report(diagnosticsContext, KotlinToolingDiagnostics.ConflictingSwiftExportDsls(projectPath))
        } else {
            collector.report(diagnosticsContext, KotlinToolingDiagnostics.DeprecatedSwiftExportDsl())
        }
    }
}
