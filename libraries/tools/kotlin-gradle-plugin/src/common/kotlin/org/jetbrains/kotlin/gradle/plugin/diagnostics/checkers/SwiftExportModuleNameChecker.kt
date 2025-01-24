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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.validateSwiftExportModuleName

internal object SwiftExportModuleNameChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (!kotlinPropertiesProvider.swiftExportEnabled) return

        AfterFinaliseDsl.await()

        val extension = multiplatformExtension?.getExtension<SwiftExportExtension>(
            SwiftExportDSLConstants.SWIFT_EXPORT_EXTENSION_NAME
        ) ?: return

        extension.moduleName.orNull?.let {
            collector.validateSwiftExportModuleName(project, it)
        }

        extension.exportedModules.orNull?.forEach { module ->
            module.moduleName.orNull?.let {
                collector.validateSwiftExportModuleName(project, it)
            }
        }
    }
}