/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.supportedAppleTargets
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.findExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironment
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.registerEmbedSwiftExportTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.initSwiftExportClasspathConfigurations
import org.jetbrains.kotlin.gradle.plugin.mpp.export.EXPORT_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension

internal object SwiftExportDSLConstants {
    const val SWIFT_EXPORT_EXTENSION_NAME = "swiftExport"
    const val TASK_GROUP = "SwiftExport"
}

internal val SetUpSwiftExportAction = KotlinProjectSetupCoroutine {
    val swiftExportExtension = multiplatformExtension.swiftExportInternal

    multiplatformExtension.addExtension(
        SwiftExportDSLConstants.SWIFT_EXPORT_EXTENSION_NAME,
        swiftExportExtension
    )

    // TODO: Move to a more generic SetUpExportAction.
    val exportExtension = objects.ExportExtension()
    multiplatformExtension.addExtension(EXPORT_EXTENSION_NAME, exportExtension)

    val appleTargets = project
        .multiplatformExtension
        .awaitTargets()
        .withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family.isAppleFamily }

    if (appleTargets.isEmpty()) return@KotlinProjectSetupCoroutine

    // The targets are awaited above, so the DSL is finalised by now and the activation is order-independent.
    if (!multiplatformExtension.isSwiftExportXcodeIntegrationActivated()) return@KotlinProjectSetupCoroutine

    initSwiftExportClasspathConfigurations()
    registerSwiftExportPipeline(swiftExportExtension)
}

/**
 * Whether the Swift Export Xcode integration has to be set up in this project.
 *
 * The `export { swift { } }` DSL activates the integration explicitly with
 * [org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportConfigurationDsl.xcodeIntegration]: an exported module
 * doesn't have to be integrated into Xcode, only the umbrella module does. The legacy `swiftExport { }` DSL has no
 * such distinction and activates the integration by being used at all.
 *
 * Reading this value is only meaningful after the DSL has been finalised, so that the order of the DSL calls
 * doesn't matter.
 */
internal fun KotlinMultiplatformExtension.isSwiftExportXcodeIntegrationActivated(): Boolean {
    val exportExtension = findExtension<ExportExtension>(EXPORT_EXTENSION_NAME)
    if (exportExtension != null && exportExtension.isSwiftExportConfigured) {
        return exportExtension.swiftExportConfiguration.activatedXcodeIntegration != null
    }

    if (isSwiftExportRequested) return true

    // TODO(KT-87989): Return false here once the legacy `swiftExport { }` DSL is deprecated. Until then projects that
    //  never ask for Swift Export keep the integration that is set up for every project with Apple targets today.
    return true
}

private fun Project.registerSwiftExportPipeline(
    swiftExportExtension: SwiftExportExtension,
) {
    val environment = XcodeEnvironment(project)

    multiplatformExtension
        .supportedAppleTargets()
        .configureEach { target ->
            setupSwiftExport(target, environment, swiftExportExtension)
        }
}

private fun Project.setupSwiftExport(
    target: KotlinNativeTarget,
    environment: XcodeEnvironment,
    swiftExportExtension: SwiftExportExtension,
) {
    registerEmbedSwiftExportTask(target, environment, swiftExportExtension)
}
