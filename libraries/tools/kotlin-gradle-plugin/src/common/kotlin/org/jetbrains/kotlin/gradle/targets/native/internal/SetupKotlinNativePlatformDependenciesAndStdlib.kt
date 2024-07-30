/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.KOTLIN_STDLIB_MODULE_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal val SetupKotlinNativePlatformDependenciesAndStdlib = KotlinProjectSetupAction {
    val kotlin = multiplatformExtensionOrNull ?: return@KotlinProjectSetupAction

    val stdlib = project.files(project.konanDistribution.stdlib)
    kotlin.targets.all { target ->
        target.compilations.all { compilation ->
            if (compilation is AbstractKotlinNativeCompilation) {
                launch { compilation.configureStdlibAndPlatformDependencies(stdlib) }
            }
        }
    }

    launch { kotlin.excludeStdlibFromNativeSourceSetDependencies() }
}

private fun AbstractKotlinNativeCompilation.configureStdlibAndPlatformDependencies(
    stdlib: FileCollection
) {
    val updatedCompileDependencyFiles = project.files().from(
        stdlib,
        compileDependencyFiles
    )

    compileDependencyFiles = updatedCompileDependencyFiles
}

/**
 * Stdlib is added from Kotlin Native Distribution in [configureStdlibAndPlatformDependencies]
 * But stdlib is also declared as dependency, to prevent from stdlib duplication, exclude it from dependency resolution
 */
private suspend fun KotlinMultiplatformExtension.excludeStdlibFromNativeSourceSetDependencies() {
    awaitSourceSets().forEach { sourceSet ->
        if (sourceSet.isNativeSourceSet.await()) {
            sourceSet.internal
                .resolvableMetadataConfiguration
                .exclude(mapOf("group" to KOTLIN_MODULE_GROUP, "module" to KOTLIN_STDLIB_MODULE_NAME))
        }
    }
}

internal val SetupKotlinNativeStdlibAndPlatformDependenciesImport = KotlinProjectSetupCoroutine {
    val multiplatform = multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine
    val sourceSets = multiplatform
        .awaitSourceSets()
        .filter { it.isNativeSourceSet.await() }
        .filterIsInstance<DefaultKotlinSourceSet>()

    val stdlib = project.files(project.konanDistribution.stdlib)
    sourceSets.forEach { sourceSet ->
        /*val commonizerTarget = */sourceSet.commonizerTarget.await() ?: return@forEach
//        val nativeDistributionDependencies = getNativeDistributionDependencies(commonizerTarget)
//        sourceSet.addDependencyForLegacyImport(nativeDistributionDependencies)
        sourceSet.addDependencyForLegacyImport(stdlib)
    }
}

internal fun Project.getNativeDistributionDependencies(target: CommonizerTarget): FileCollection {
    return when (target) {
        is LeafCommonizerTarget -> getOriginalPlatformLibrariesFor(target)
        is SharedCommonizerTarget -> {
            val klibs = project.commonizedNativeDistributionKlibsOrNull(target) ?: return project.files()
            project.files(klibs)
        }
    }
}

private fun Project.getOriginalPlatformLibrariesFor(target: LeafCommonizerTarget): FileCollection =
    getOriginalPlatformLibrariesFor(target.konanTarget)

internal fun Project.getOriginalPlatformLibrariesFor(konanTarget: KonanTarget): FileCollection = project.filesProvider {
    konanDistribution.platformLibsDir.resolve(konanTarget.name).listLibraryFiles().toSet()
}

private fun File.listLibraryFiles(): List<File> = listFiles().orEmpty()
    .filter { it.isDirectory || it.extension == "klib" }

/**
 * Legacy resolves [implementationMetadataConfigurationName] and [intransitiveMetadataConfigurationName]
 * to get dependencies for given source set. Therefore, compileDependencyFiles and dependencies in those configurations
 * must be synced.
 */
private fun DefaultKotlinSourceSet.addDependencyForLegacyImport(libraries: FileCollection) {
    @Suppress("DEPRECATION")
    val metadataConfigurationName = if (project.isIntransitiveMetadataConfigurationEnabled) {
        intransitiveMetadataConfigurationName
    } else {
        implementationMetadataConfigurationName
    }
    project.dependencies.add(metadataConfigurationName, libraries)
}
