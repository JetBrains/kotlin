/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation

internal suspend fun Project.configureCInteropCommonizer() {
    val interopTask = commonizeCInteropTask() ?: return

    val cinteropGroups = getCInteropGroups()
    configureCInteropCommonizerConsumableConfigurations(cinteropGroups, interopTask)

    interopTask.configure {
        it.allInteropGroups.complete(cinteropGroups)
    }
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
                artifact.extension = "klib"
                artifact.type = "klib"
                artifact.classifier = "cinterop-" + sharedCommonizerTargets.dashedIdentityString()
                artifact.builtBy(interopTask)
            }
        }
    }
}

private suspend fun Project.allCinteropCommonizerDependents(): Set<CInteropCommonizerDependent> {
    val multiplatformExtension = project.multiplatformExtensionOrNull ?: return emptySet()

    val fromSharedNativeCompilations = multiplatformExtension
        .targets.flatMap { target -> target.compilations }
        .filterIsInstance<KotlinSharedNativeCompilation>()
        .mapNotNull { compilation -> CInteropCommonizerDependent.from(compilation) }
        .toSet()

    val fromSourceSets = multiplatformExtension.awaitSourceSets()
        .mapNotNull { sourceSet -> CInteropCommonizerDependent.from(sourceSet) }
        .toSet()

    val fromSourceSetsAssociateCompilations = multiplatformExtension.awaitSourceSets()
        .mapNotNull { sourceSet -> CInteropCommonizerDependent.fromAssociateCompilations(sourceSet) }
        .toSet()

    return (fromSharedNativeCompilations + fromSourceSets + fromSourceSetsAssociateCompilations)
}

private suspend fun Project.getCInteropGroups(): Set<CInteropCommonizerGroup> {
    val dependents = allCinteropCommonizerDependents()

    val allScopeSets = dependents.map { it.scopes }.toSet()
    val rootScopeSets = allScopeSets.filter { scopeSet ->
        allScopeSets.none { otherScopeSet -> otherScopeSet != scopeSet && otherScopeSet.containsAll(scopeSet) }
    }

    val result = rootScopeSets.map { scopeSet ->
        val dependentsForScopes = dependents.filter { dependent ->
            scopeSet.containsAll(dependent.scopes)
        }

        CInteropCommonizerGroup(
            targets = dependentsForScopes.map { it.target }.toSet(),
            interops = dependentsForScopes.flatMap { it.interops }.toSet()
        )
    }.toSet()

    return result
}
