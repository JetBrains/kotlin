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
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal fun Project.setUpHierarchicalKotlinNativePlatformDependencies() {
    val kotlin = multiplatformExtensionOrNull ?: return
    kotlin.sourceSets.forEach { sourceSet ->
        val target = getCommonizerTarget(sourceSet) ?: return@forEach
        val targetDependencies = getDependenciesFor(target)
        val stdlib = project.filesProvider { listOf(konanDistribution.stdlib) }
        addDependencies(sourceSet, targetDependencies)
        addDependencies(sourceSet, stdlib)
    }
}

private fun Project.getDependenciesFor(target: CommonizerTarget): FileCollection {
    val commonizerTask = commonizeNativeDistributionHierarchicallyTask?.get() ?: return project.files()

    /* Target is participating in any commonization and will therefore receive dependencies provided by the commonizer */
    if (commonizerTask.rootCommonizerTargets.any { rootTarget -> rootTarget.isEqualOrAncestorOf(target) }) {
        return commonizerTask.getCommonizedDependenciesFor(target)
    }

    /* Target does not participate in any commonization. Try finding relevant platform libraries */
    val konanTarget = target.konanTargets.singleOrNull() ?: return project.files()
    return project.filesProvider { konanDistribution.platformLibsDir.resolve(konanTarget.name).listFiles().orEmpty().toSet() }
}

private fun HierarchicalNativeDistributionCommonizerTask.getCommonizedDependenciesFor(target: CommonizerTarget): FileCollection {
    val rootTarget = rootCommonizerTargets.firstOrNull { rootTarget -> rootTarget.isEqualOrAncestorOf(target) } ?: return project.files()
    val targetOutputDirectory = HierarchicalCommonizerOutputLayout.getTargetDirectory(getRootOutputDirectory(rootTarget), target)
    val targetDependencies = project.filesProvider { targetOutputDirectory.listFiles().orEmpty().toList() }.builtBy(this)

    /*
    LeafCommonizerTargets will still analyze against 'new' 'commonized' platform dependencies.
    This means, that some parts of the API might get lifted to the direct parent dependency.
     */
    val necessaryParentTargetDependencies: FileCollection = rootTarget.takeIf { target is LeafCommonizerTarget }
        ?.findParentOf(target)
        ?.let { parentTarget -> getCommonizedDependenciesFor(parentTarget) }
        ?: project.files()

    return targetDependencies + necessaryParentTargetDependencies
}

private fun CommonizerTarget.findParentOf(target: CommonizerTarget): CommonizerTarget? {
    return withAllAncestors()
        .filterIsInstance<SharedCommonizerTarget>()
        .firstOrNull { target in it.targets }
}

private fun Project.addDependencies(sourceSet: KotlinSourceSet, libraries: FileCollection) {
    getMetadataCompilationForSourceSet(sourceSet)?.let { compilation ->
        compilation.compileDependencyFiles += libraries
    }
    if (sourceSet is DefaultKotlinSourceSet) {
        val metadataConfigurationName =
            if (project.isIntransitiveMetadataConfigurationEnabled) sourceSet.intransitiveMetadataConfigurationName
            else sourceSet.implementationMetadataConfigurationName
        dependencies.add(metadataConfigurationName, libraries)
    }
}

private val Project.konanDistribution: KonanDistribution
    get() = KonanDistribution(project.file(konanHome))
