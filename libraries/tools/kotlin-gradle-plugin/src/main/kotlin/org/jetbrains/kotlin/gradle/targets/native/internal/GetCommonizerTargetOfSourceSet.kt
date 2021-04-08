/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil.compilationsBySourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.konan.target.KonanTarget

internal fun Project.getCommonizerTarget(sourceSet: KotlinSourceSet): CommonizerTarget? {
    val kotlin = multiplatformExtensionOrNull ?: return null
    val sourceSetsDependingOnThisSourceSet = kotlin.resolveSourceSetsDirectlyDependingOn(sourceSet)
    if (sourceSetsDependingOnThisSourceSet.isEmpty()) {
        return getLeafCommonizerTarget(sourceSet)
    }

    val dependeeCommonizerTargets = sourceSetsDependingOnThisSourceSet
        .map { sourceSetDependingOnThisSourceSet -> getCommonizerTarget(sourceSetDependingOnThisSourceSet) ?: return null }
        .toSet()

    return when {
        dependeeCommonizerTargets.isEmpty() -> throw IllegalStateException()
        dependeeCommonizerTargets.size == 1 -> dependeeCommonizerTargets.single()
        else -> SharedCommonizerTarget(dependeeCommonizerTargets)
    }
}

private fun Project.getLeafCommonizerTarget(sourceSet: KotlinSourceSet): LeafCommonizerTarget? {
    val konanTargets = compilationsBySourceSets(this)[sourceSet].orEmpty()
        .flatMap { compilation -> compilation.konanTargets }

    return if (konanTargets.size == 1) LeafCommonizerTarget(konanTargets.single())

    /*
    Can even be more than one, when added using `KotlinCompilation.source`. Still returning null, since this
    does not represent a 'proper' source set hierarchy
     */
    else null
}

private fun KotlinSourceSetContainer.resolveSourceSetsDirectlyDependingOn(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
    val dependeeSourceSets = sourceSets
        .filter { candidateSourceSet -> candidateSourceSet != sourceSet }
        .filter { candidateSourceSet -> sourceSet in candidateSourceSet.dependsOn }

    fun isTransitiveDependee(sourceSet: KotlinSourceSet): Boolean {
        return sourceSet.dependsOn.any { it in dependeeSourceSets || isTransitiveDependee(it) }
    }

    return dependeeSourceSets.filterNot(::isTransitiveDependee).toSet()
}

private val KotlinCompilation<*>.konanTargets: Set<KonanTarget>
    get() {
        return when (this) {
            is KotlinSharedNativeCompilation -> konanTargets.toSet()
            is KotlinNativeCompilation -> setOf(konanTarget)
            else -> emptySet()
        }
    }
