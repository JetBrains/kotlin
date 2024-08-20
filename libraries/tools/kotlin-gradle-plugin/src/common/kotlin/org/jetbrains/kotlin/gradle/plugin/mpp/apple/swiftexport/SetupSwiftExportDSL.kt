/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.supportedAppleTargets
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.plugin.mpp.StaticLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironment
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.registerEmbedSwiftExportTask
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl

internal object SwiftExportDSLConstants {
    const val SWIFT_EXPORT_LIBRARY_PREFIX = "swiftExport"
    const val SWIFT_EXPORT_EXTENSION_NAME = "swiftExport"
    const val TASK_GROUP = "SwiftExport"
}

@ExperimentalSwiftExportDsl
internal val SetUpSwiftExportAction = KotlinProjectSetupAction {
    if (!kotlinPropertiesProvider.swiftExportEnabled) return@KotlinProjectSetupAction
    warnAboutExperimentalSwiftExportFeature()
    val swiftExportExtension = objects.SwiftExportExtension(dependencies)

    multiplatformExtension.addExtension(
        SwiftExportDSLConstants.SWIFT_EXPORT_EXTENSION_NAME,
        swiftExportExtension
    )

    registerSwiftExportPipeline(swiftExportExtension)
}

private fun Project.warnAboutExperimentalSwiftExportFeature() {
    reportDiagnosticOncePerBuild(
        KotlinToolingDiagnostics.ExperimentalFeatureWarning("Swift Export", "https://kotl.in/1cr522")
    )
}

private fun Project.registerSwiftExportPipeline(
    swiftExportExtension: SwiftExportExtension,
) {
    val environment = XcodeEnvironment(project)

    multiplatformExtension
        .supportedAppleTargets()
        .configureEach { target ->
            target.binaries.staticLib(SwiftExportDSLConstants.SWIFT_EXPORT_LIBRARY_PREFIX) {
                setupSwiftExport(this, environment, swiftExportExtension)
            }
        }
}

private fun Project.setupSwiftExport(
    library: StaticLibrary,
    environment: XcodeEnvironment,
    swiftExportExtension: SwiftExportExtension,
) {
    swiftExportExtension.addBinary(library)
    registerEmbedSwiftExportTask(library, environment, swiftExportExtension)
}