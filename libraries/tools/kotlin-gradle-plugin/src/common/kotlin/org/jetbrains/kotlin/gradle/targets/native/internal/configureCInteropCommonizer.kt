/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout

internal suspend fun Project.configureCInteropCommonizer() {
    val interopTask = commonizeCInteropTask() ?: return
    configureCInteropCommonizerConsumableConfigurations(kotlinCInteropGroups.await(), interopTask)
}

private fun Project.configureCInteropCommonizerConsumableConfigurations(
    cinteropGroups: Set<CInteropCommonizerGroup>,
    interopTask: TaskProvider<CInteropCommonizerTask>,
) {
    for (commonizerGroup in cinteropGroups) {
        for (sharedCommonizerTargets in commonizerGroup.targets) {
            val configuration = locateOrCreateCommonizedCInteropApiElementsConfiguration(sharedCommonizerTargets)
            val commonizerTargetOutputDir = interopTask.map { task ->
                CommonizerOutputFileLayout.resolveCommonizedDirectory(task.outputDirectory(commonizerGroup), sharedCommonizerTargets)
            }

            project.artifacts.add(configuration.name, commonizerTargetOutputDir) { artifact ->
                artifact.extension = CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR
                artifact.type = CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR
                artifact.builtBy(interopTask)
            }
        }
    }
}
