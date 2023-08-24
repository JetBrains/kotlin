/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.getOrPut
import org.jetbrains.kotlin.gradle.utils.lazyFuture

/**
 * Represents a group of cinterops and targets that can be passed to a single invocation to the commonizer.
 * CInterops are bundled into these groups to reduce the amount of commonizer invocations and therefore ensuring no duplicate work
 * has to be done.
 */
internal data class CInteropCommonizerGroup(
    @get:Input val targets: Set<SharedCommonizerTarget>,
    @get:Input val interops: Set<CInteropIdentifier>,
) {
    override fun toString(): String {
        return buildString {
            appendLine("InteropsGroup {")
            appendLine("targets: ")
            targets.sortedBy { it.targets.size }.forEach { target ->
                appendLine("    $target")
            }
            appendLine()
            appendLine("interops: ")
            interops.sortedBy { it.toString() }.forEach { interop ->
                appendLine("    $interop")
            }
            appendLine("}")
        }
    }
}

/* Collection of cinterop groups present in the given project */

/**
 * Represents all collected [CInteropCommonizerGroup] gruops for the given project
 */
internal val Project.kotlinCInteropGroups: Future<Set<CInteropCommonizerGroup>>
    get() = extensions.extraProperties.getOrPut("org.jetbrains.kotlin.gradle.targets.native.internal.kotlinCInteropGroups") {
        lazyFuture { collectCInteropGroups() }
    }

private suspend fun Project.collectCInteropGroups(): Set<CInteropCommonizerGroup> {
    val dependents = allCinteropCommonizerDependents()

    val allScopeSets = dependents.map { it.scopes }.toSet()
    val rootScopeSets = allScopeSets.filter { scopeSet ->
        allScopeSets.none { otherScopeSet -> otherScopeSet != scopeSet && otherScopeSet.containsAll(scopeSet) }
    }

    return rootScopeSets.map { scopeSet ->
        val dependentsForScopes = dependents.filter { dependent ->
            scopeSet.containsAll(dependent.scopes)
        }

        CInteropCommonizerGroup(
            targets = dependentsForScopes.map { it.target }.toSet(),
            interops = dependentsForScopes.flatMap { it.interops }.toSet()
        )
    }.toSet()

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

/**
 * Utility function that allows to find the corresponding [CInteropCommonizerGroup] for a given [CInteropCommonizerDependent]
 */
internal suspend fun Project.findCInteropCommonizerGroup(dependent: CInteropCommonizerDependent): CInteropCommonizerGroup? {
    val suitableGroups = kotlinCInteropGroups.await().filter { group ->
        group.interops.containsAll(dependent.interops) && group.targets.contains(dependent.target)
    }

    assert(suitableGroups.size <= 1) {
        "CInteropCommonizerTask: Unnecessary work detected: More than one suitable group found for cinterop dependent."
    }

    return suitableGroups.firstOrNull()
}
