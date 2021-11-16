/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.allLeaves
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil.compilationsBySourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation

internal fun Project.getCommonizerTarget(sourceSet: KotlinSourceSet): CommonizerTarget? {
    val allCompilationLeafTargets = compilationsBySourceSets(this)[sourceSet].orEmpty()
        .filter { compilation -> compilation !is KotlinMetadataCompilation }
        .map { compilation -> getCommonizerTarget(compilation) ?: return null }
        .allLeaves()

    return when {
        allCompilationLeafTargets.isEmpty() -> null
        allCompilationLeafTargets.size == 1 -> allCompilationLeafTargets.single()
        else -> SharedCommonizerTarget(allCompilationLeafTargets)
    }
}

internal fun Project.getSharedCommonizerTarget(sourceSet: KotlinSourceSet): SharedCommonizerTarget? {
    return getCommonizerTarget(sourceSet) as? SharedCommonizerTarget
}
