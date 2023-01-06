/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheckWhenEvaluated
import org.jetbrains.kotlin.konan.target.KonanTarget

internal fun checkAndReportDeprecatedNativeTargets(project: Project) {
    project.runProjectConfigurationHealthCheckWhenEvaluated {
        val targets = project.extensions.getByType(KotlinMultiplatformExtension::class.java).targets
        val usedDeprecatedTargets = targets.filter { it is KotlinNativeTarget && it.konanTarget in KonanTarget.deprecatedTargets }
        if (usedDeprecatedTargets.isEmpty()) return@runProjectConfigurationHealthCheckWhenEvaluated
        SingleWarningPerBuild.show(
            project,
            "w: The following deprecated kotlin native targets were used in the project: ${usedDeprecatedTargets.joinToString { it.targetName }}"
        )
    }
}