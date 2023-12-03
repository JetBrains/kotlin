/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffectCoroutine
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import java.io.File

internal val SetupKotlinNativePlatformDependenciesAndStdlib =
    KotlinCompilationSideEffectCoroutine<AbstractKotlinNativeCompilation> { compilation ->
        val project = compilation.project

        // Commonizer target must not be null for AbstractKotlinNativeCompilation, but we are graceful here and just return
        val commonizerTarget = compilation.commonizerTarget.await() ?: return@KotlinCompilationSideEffectCoroutine
        val nativeDistributionDependencies = project.getNativeDistributionDependencies(commonizerTarget)
        compilation.compileDependencyFiles += nativeDistributionDependencies

        val stdlib = project.files(project.konanDistribution.stdlib)
        compilation.compileDependencyFiles += stdlib
    }

internal val SetupKotlinNativePlatformDependenciesForLegacyImport = KotlinProjectSetupCoroutine {
    val multiplatform = multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine
    val sourceSets = multiplatform
        .awaitSourceSets()
        .filter { it.isNativeSourceSet.await() }
        .filterIsInstance<DefaultKotlinSourceSet>()

    val stdlib = project.files(project.konanDistribution.stdlib)
    sourceSets.forEach { sourceSet ->
        val commonizerTarget = sourceSet.commonizerTarget.await() ?: return@forEach
        val nativeDistributionDependencies = getNativeDistributionDependencies(commonizerTarget)
        sourceSet.addDependencyForLegacyImport(nativeDistributionDependencies)
        sourceSet.addDependencyForLegacyImport(stdlib)
    }
}

internal fun Project.getNativeDistributionDependencies(target: CommonizerTarget): FileCollection {
    return when (target) {
        is LeafCommonizerTarget -> getOriginalPlatformLibrariesFor(target)
        is SharedCommonizerTarget -> {
            val commonizerTaskProvider = commonizeNativeDistributionTask ?: return project.files()
            val commonizedLibrariesProvider = commonizerTaskProvider.flatMap { task ->
                task.rootOutputDirectoryProperty.map { getCommonizedPlatformLibrariesFor(it.asFile, target) }
            }
            project.files(commonizedLibrariesProvider)
        }
    }
}

private fun Project.getOriginalPlatformLibrariesFor(target: LeafCommonizerTarget): FileCollection = project.filesProvider {
    konanDistribution.platformLibsDir.resolve(target.konanTarget.name).listLibraryFiles().toSet()
}

private fun getCommonizedPlatformLibrariesFor(rootOutputDirectory: File, target: SharedCommonizerTarget): List<File> {
    val targetOutputDirectory = CommonizerOutputFileLayout.resolveCommonizedDirectory(rootOutputDirectory, target)
    return targetOutputDirectory.listLibraryFiles()
}

private fun File.listLibraryFiles(): List<File> = listFiles().orEmpty()
    .filter { it.isDirectory || it.extension == "klib" }

/**
 * Legacy resolves [implementationMetadataConfigurationName] and [intransitiveMetadataConfigurationName]
 * to get dependencies for given source set. Therefore, compileDependencyFiles and dependencies in those configurations
 * must be synced.
 */
private fun DefaultKotlinSourceSet.addDependencyForLegacyImport(libraries: FileCollection) {
    val metadataConfigurationName =
        if (project.isIntransitiveMetadataConfigurationEnabled) intransitiveMetadataConfigurationName
        else implementationMetadataConfigurationName
    project.dependencies.add(metadataConfigurationName, libraries)
}
