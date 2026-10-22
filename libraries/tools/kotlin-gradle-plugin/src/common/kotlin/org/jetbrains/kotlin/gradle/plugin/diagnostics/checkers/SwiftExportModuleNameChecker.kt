/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportDSLConstants
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.normalizedSwiftExportModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.validateSwiftExportModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.export.EXPORT_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension

internal object SwiftExportModuleNameChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        AfterFinaliseDsl.await()

        multiplatformExtension?.getExtension<ExportExtension>(EXPORT_EXTENSION_NAME)
            ?.swiftExportConfiguration
            ?.moduleName?.orNull?.let {
                collector.validateSwiftExportModuleName(project, it)
            }

        val legacyExtension = multiplatformExtension?.getExtension<SwiftExportExtension>(
            SwiftExportDSLConstants.SWIFT_EXPORT_EXTENSION_NAME
        ) ?: return

        legacyExtension.moduleName.orNull?.let {
            collector.validateSwiftExportModuleName(project, it)
        }

        legacyExtension.exportedModules.orNull?.forEach { module ->
            module.moduleName.orNull?.let {
                collector.validateSwiftExportModuleName(project, it)
            }
        }
    }
}
