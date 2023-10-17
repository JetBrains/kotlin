/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

internal val MultiplatformBuildStatsReportSetupAction = KotlinProjectSetupAction {
    multiplatformExtension.targets.all { target ->
        /* Report the platform to tbe build stats service */
        val targetName = if (target is KotlinNativeTarget) target.konanTarget.name
        else target.platformType.name
        KotlinBuildStatsService.getInstance()?.report(StringMetrics.MPP_PLATFORMS, targetName)
    }
}
