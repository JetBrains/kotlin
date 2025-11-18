/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.supportedAppleTargets
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironment
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.registerEmbedSwiftExportTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.initSwiftExportClasspathConfigurations
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactoryProvider

internal object SwiftExportDSLConstants {
    const val SWIFT_EXPORT_EXTENSION_NAME = "swiftExport"
    const val TASK_GROUP = "SwiftExport"
}

internal val SetUpSwiftExportAction = KotlinProjectSetupCoroutine {
    val swiftExportExtension = objects.SwiftExportExtension(
        dependencies,
        variantImplementationFactoryProvider(),
    ) { path -> project.project(path) }

    multiplatformExtension.addExtension(
        SwiftExportDSLConstants.SWIFT_EXPORT_EXTENSION_NAME,
        swiftExportExtension
    )

    val appleTargets = project
        .multiplatformExtension
        .awaitTargets()
        .withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family.isAppleFamily }

    if (appleTargets.isEmpty()) return@KotlinProjectSetupCoroutine

    initSwiftExportClasspathConfigurations()
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