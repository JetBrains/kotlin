/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.utils.*

/**
 * Will detect usage of "Android Style" source directories (like 'src/main/kotlin') and emit a warning
 */
internal object MultiplatformLayoutV2AndroidStyleSourceDirUsageChecker : KotlinAndroidSourceSetLayoutChecker {

    override fun checkCreatedSourceSet(
        diagnosticsCollector: KotlinToolingDiagnosticsCollector,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
    ) {
        if (target.project.kotlinPropertiesProvider.ignoreMppAndroidSourceSetLayoutV2AndroidStyleDirs) return
        val projectRoot = target.project.rootDir
        val androidStyleSourceDir = target.project.file("src/${androidSourceSet.name}/kotlin")
        if (androidStyleSourceDir in kotlinSourceSet.kotlin.srcDirs && androidStyleSourceDir.exists()) {
            val kotlinStyleSourceDirToUse = target.project.file("src/${kotlinSourceSet.name}/kotlin")
            diagnosticsCollector.report(
                target.project,
                KotlinToolingDiagnostics.AndroidStyleSourceDirUsageWarning(
                    androidStyleSourceDir.relativeTo(projectRoot).toString(),
                    kotlinStyleSourceDirToUse.relativeTo(projectRoot).toString(),
                )
            )
        }
    }
}
