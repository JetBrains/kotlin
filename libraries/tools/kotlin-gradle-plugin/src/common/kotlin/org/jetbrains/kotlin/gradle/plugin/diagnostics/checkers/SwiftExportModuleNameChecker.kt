/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportDSLConstants
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension
import org.jetbrains.kotlin.gradle.utils.dashSeparatedToUpperCamelCase

internal object SwiftExportModuleNameChecker : KotlinGradleProjectChecker {

    private val moduleNameRegex by lazy { Regex("^[A-Za-z0-9_]+$") }

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (!kotlinPropertiesProvider.swiftExportEnabled) return
        AfterFinaliseDsl.await()

        val swiftExportExtension = multiplatformExtension?.getExtension<SwiftExportExtension>(
            SwiftExportDSLConstants.SWIFT_EXPORT_EXTENSION_NAME
        )

        // Check root module name
        swiftExportExtension?.moduleName?.orNull?.let { moduleName ->
            project.checkModuleName(moduleName, collector)
        }

        // Check exported module names
        swiftExportExtension?.exportedModules?.orNull?.forEach { module ->
            module.moduleName.orNull?.let { moduleName ->
                project.checkModuleName(moduleName, collector)
            }

            // Check module version names
            val moduleVersionName = dashSeparatedToUpperCamelCase(module.moduleVersion.name)
            project.checkModuleName(moduleVersionName, collector)
        }
    }

    private fun Project.checkModuleName(moduleName: String, collector: KotlinToolingDiagnosticsCollector) {
        if (!moduleNameRegex.matches(moduleName)) {
            collector.report(project, KotlinToolingDiagnostics.SwiftExportInvalidModuleName(moduleName))
        }
    }
}