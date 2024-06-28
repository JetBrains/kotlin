/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.targets.android.findAndroidTarget
import org.jetbrains.kotlin.gradle.utils.findAppliedAndroidPluginIdOrNull

internal object AndroidPluginWithoutAndroidTargetChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        project.configurationResult.await()
        if (multiplatformExtension == null || kotlinPropertiesProvider.ignoreAbsentAndroidMultiplatformTarget) return

        val androidPluginId = project.findAppliedAndroidPluginIdOrNull() ?: return
        if (project.findAndroidTarget() != null) return
        collector.reportOncePerGradleProject(
            project,
            KotlinToolingDiagnostics.AndroidTargetIsMissing(project.name, project.path, androidPluginId)
        )
    }
}
