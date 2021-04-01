/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal fun Project.setUpHierarchicalKotlinNativePlatformDependencies() {
    val task = commonizeNativeDistributionHierarchicalTask?.get() ?: return
    val kotlin = multiplatformExtensionOrNull ?: return
    kotlin.sourceSets.forEach { sourceSet ->
        val target = getCommonizerTarget(sourceSet) ?: return@forEach
        val commonizedDependencies = task.dependenciesFor(target)
        val stdlib = project.filesProvider { listOf(konanDistribution.stdlib) }
        addDependencies(sourceSet, commonizedDependencies)
        addDependencies(sourceSet, stdlib)
    }
}

private fun HierarchicalNativeDistributionCommonizerTask.dependenciesFor(target: CommonizerTarget): FileCollection {
    val rootTarget = rootCommonizerTargets.firstOrNull { rootTarget -> rootTarget.isEqualOrAncestorOf(target) } ?: return project.files()
    val targetOutputDirectory = HierarchicalCommonizerOutputLayout.getTargetDirectory(getRootOutputDirectory(rootTarget), target)
    val targetDependencies = project.filesProvider { targetOutputDirectory.listFiles().orEmpty().toList() }.builtBy(this)

    if (target is LeafCommonizerTarget) {
        val expectTarget = rootTarget.withAllAncestors()
            .firstOrNull { (it as? SharedCommonizerTarget)?.targets?.contains(target) == true }
            ?: return targetDependencies

        return targetDependencies + dependenciesFor(expectTarget)
    }

    return targetDependencies
}

private fun Project.addDependencies(sourceSet: KotlinSourceSet, libraries: FileCollection) {
    getMetadataCompilationForSourceSet(sourceSet)?.let { compilation ->
        compilation.compileDependencyFiles += libraries
    }
    dependencies.add(sourceSet.implementationMetadataConfigurationName, libraries)
}

private val Project.konanDistribution: KonanDistribution
    get() = KonanDistribution(project.file(konanHome))
