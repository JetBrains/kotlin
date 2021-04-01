/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.HierarchicalCommonizerOutputLayout
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.isAncestorOf
import org.jetbrains.kotlin.commonizer.stdlib
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

        val rootTarget = task.rootCommonizerTargets
            .firstOrNull { rootTarget -> rootTarget == target || rootTarget.isAncestorOf(target) }
            ?: return@forEach

        val rootOutputDirectory = task.getRootOutputDirectory(rootTarget)
        val targetOutputDirectory = HierarchicalCommonizerOutputLayout.getTargetDirectory(rootOutputDirectory, target)

        val dependencies = project.filesProvider { targetOutputDirectory.listFiles().orEmpty().toList() }.builtBy(task)
        val stdlib = project.filesProvider { listOf(konanDistribution.stdlib) }
        addDependencies(sourceSet, dependencies)
        addDependencies(sourceSet, stdlib)
    }
}

private fun Project.addDependencies(sourceSet: KotlinSourceSet, libraries: FileCollection) {
    getMetadataCompilationForSourceSet(sourceSet)?.let { compilation ->
        compilation.compileDependencyFiles += libraries
    }
    dependencies.add(sourceSet.implementationMetadataConfigurationName, libraries)
}

private val Project.konanDistribution: KonanDistribution
    get() = KonanDistribution(project.file(konanHome))
