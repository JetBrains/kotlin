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
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.tasks.withType
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

    excludeDefaultPlatformDependenciesFromKotlinNativeCompileTasks()
    launch { kotlin.excludeStdlibFromNativeSourceSetDependencies() }
}

private suspend fun AbstractKotlinNativeCompilation.configureStdlibAndPlatformDependencies(
    stdlib: FileCollection
) {
    // Commonizer target must not be null for AbstractKotlinNativeCompilation, but we are graceful here and just return
    val commonizerTarget = commonizerTarget.await() ?: return
    val nativeDistributionDependencies = project.getNativeDistributionDependencies(commonizerTarget)

    val updatedCompileDependencyFiles = project.files().from(
        stdlib,
        nativeDistributionDependencies,
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
            project.locateOrRegisterMetadataDependencyTransformationTask(sourceSet).configure { task ->
                task.excludeModules(group = KOTLIN_MODULE_GROUP, module = KOTLIN_STDLIB_MODULE_NAME)
            }
        }
    }
}

/**
 * Platform dependencies are added to compilation "compile files" in [configureStdlibAndPlatformDependencies]
 * So user code that integrates with Kotlin Native Compilations can safely rely on that classpath.
 * However, for performance optimization reasons, Kotlin Native automatically loads Platform Dependencies from its distribution.
 * And because of that KGP has to explicitly filter out these platform dependencies from tasks.
 *
 * NB: This is not applicable for Native Shared Metadata Compilation, they will receive commonized versions of platform libs.
 */
private fun Project.excludeDefaultPlatformDependenciesFromKotlinNativeCompileTasks() {
    tasks.withType<KotlinNativeLink>().configureEach { task ->
        @Suppress("DEPRECATION")
        val konanTarget = task.compilation.konanTarget
        task.excludeOriginalPlatformLibraries = task.project.getOriginalPlatformLibrariesFor(konanTarget)
    }
    tasks.withType<KotlinNativeCompile>().configureEach { task ->
        // metadata compilations should have commonized platform libraries in the classpath i.e. they are not "original"
        if (task.isMetadataCompilation) return@configureEach
        val konanTarget = task.konanTarget
        task.excludeOriginalPlatformLibraries = task.project.getOriginalPlatformLibrariesFor(konanTarget)
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

private fun Project.getOriginalPlatformLibrariesFor(target: LeafCommonizerTarget): FileCollection =
    getOriginalPlatformLibrariesFor(target.konanTarget)

private fun Project.getOriginalPlatformLibrariesFor(konanTarget: KonanTarget): FileCollection = project.filesProvider {
    konanDistribution.platformLibsDir.resolve(konanTarget.name).listLibraryFiles().toSet()
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
    @Suppress("DEPRECATION")
    val metadataConfigurationName = if (project.isIntransitiveMetadataConfigurationEnabled) {
        intransitiveMetadataConfigurationName
    } else {
        implementationMetadataConfigurationName
    }
    project.dependencies.add(metadataConfigurationName, libraries)
}
