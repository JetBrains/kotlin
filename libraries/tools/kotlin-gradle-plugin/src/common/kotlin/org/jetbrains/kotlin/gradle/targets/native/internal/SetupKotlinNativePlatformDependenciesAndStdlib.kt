/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
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
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeFromToolchainProvider
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


private suspend fun AbstractKotlinNativeCompilation.configureStdlibAndPlatformDependencies(
    stdlib: FileCollection,
) {

    val commonizerTarget = commonizerTarget.await() ?: return
    val nativeBundleBuildService = KotlinNativeBundleBuildService.registerIfAbsent(project)
    val nativeDependency = KotlinNativeBundleBuildService.getNativeDistributionDependencies(
        project,
        commonizerTarget,
        nativeBundleBuildService
    )

    val updatedCompileDependencyFiles = project.files().from(
        stdlib,
        nativeDependency,
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
    val nativeBundleBuildService = KotlinNativeBundleBuildService.registerIfAbsent(project)

    sourceSets.forEach { sourceSet ->
        val commonizerTarget = sourceSet.commonizerTarget.await() ?: return@forEach
        val konanDistributionProvider = KotlinNativeFromToolchainProvider(
            project,
            commonizerTarget.konanTargets,
            nativeBundleBuildService
        ).bundleDirectory.map { KonanDistribution(it) }
        val nativeDistributionDependencies = getNativeDistributionDependencies(konanDistributionProvider, commonizerTarget)
        sourceSet.addDependencyForLegacyImport(nativeDistributionDependencies)
        sourceSet.addDependencyForLegacyImport(stdlib)
    }
}

internal fun Project.getNativeDistributionDependencies(konanDistribution: Provider<KonanDistribution>, target: CommonizerTarget): FileCollection {
    return when (target) {
        is LeafCommonizerTarget -> project.objects.getOriginalPlatformLibrariesFor(
            konanDistribution,
            target
        )
        is SharedCommonizerTarget -> {
            val klibs = project.commonizedNativeDistributionKlibsOrNull(target) ?: return objects.fileCollection()
            objects.fileCollection().from(klibs)
        }
    }
}

private fun ObjectFactory.getOriginalPlatformLibrariesFor(
    konanDistribution: Provider<KonanDistribution>,
    target: LeafCommonizerTarget,
): FileCollection =
    getOriginalPlatformLibrariesFor(konanDistribution, target.konanTarget)

internal fun ObjectFactory.getOriginalPlatformLibrariesFor(
    konanDistribution: Provider<KonanDistribution>,
    konanTarget: KonanTarget,
): FileCollection =
    fileCollection()
        .from(
            konanDistribution.map { it.platformLibsDir.resolve(konanTarget.name).listLibraryFiles().toSet() }
        )


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
