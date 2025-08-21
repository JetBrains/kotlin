/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.supportedAppleTargets
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironment
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.registerEmbedSwiftExportTask

internal object SwiftExportDSLConstants {
    const val SWIFT_EXPORT_EXTENSION_NAME = "swiftExport"
    const val TASK_GROUP = "SwiftExport"
}

internal val SetUpSwiftExportAction = KotlinProjectSetupAction {
    val swiftExportExtension = objects.SwiftExportExtension(dependencies)

    multiplatformExtension.addExtension(
        SwiftExportDSLConstants.SWIFT_EXPORT_EXTENSION_NAME,
        swiftExportExtension
    )

    registerSwiftExportPipeline(swiftExportExtension)
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