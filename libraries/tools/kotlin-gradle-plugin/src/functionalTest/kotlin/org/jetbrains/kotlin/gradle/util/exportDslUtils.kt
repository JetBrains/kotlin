/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExportDsl::class)

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.export.EXPORT_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension
import org.jetbrains.kotlin.gradle.unitTests.utils.applyEmbedAndSignEnvironment
import kotlin.test.assertNotNull

internal const val EMBED_SWIFT_EXPORT_TASK_NAME = "embedSwiftExportForXcode"

internal val Project.exportExtension: ExportExtension
    get() = assertNotNull(
        multiplatformExtension.getExtension(EXPORT_EXTENSION_NAME),
        "Expected the `$EXPORT_EXTENSION_NAME` extension to be registered on the multiplatform extension"
    )

/**
 * An evaluated multiplatform project for testing the `export { }` DSL.
 *
 * The default target is `iosSimulatorArm64` because [applyEmbedAndSignEnvironment] uses the matching sdk and archs.
 */
internal fun exportDslProject(
    withXcodeEnvironment: Boolean = false,
    multiplatform: KotlinMultiplatformExtension.() -> Unit = { iosSimulatorArm64() },
    configureExport: Project.() -> Unit = {},
): ProjectInternal = buildProjectWithMPP(
    preApplyCode = {
        if (withXcodeEnvironment) {
            applyEmbedAndSignEnvironment(configuration = "DEBUG", sdk = "iphonesimulator", archs = "arm64")
        }
        configureRepositoriesForTests()
    },
    code = {
        kotlin { multiplatform() }
        configureExport()
    }
).also { it.evaluate() }
