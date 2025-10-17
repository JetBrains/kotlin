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
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal val SetupKotlinNativePlatformDependenciesAndStdlib = KotlinProjectSetupAction {
    val kotlin = multiplatformExtensionOrNull ?: return@KotlinProjectSetupAction

    val stdlib = project.files(project.konanDistribution.stdlib)
    kotlin.targets.all { target ->
        target.compilations.all { compilation ->
            if (compilation is KotlinSharedNativeCompilation) {
                launch {
                    compilation.configureStdlibAndPlatformDependencies(
                        stdlib,
                        AbstractKotlinNativeCompilation::retrievePlatformDependenciesWithNativeDistribution
                    )
                }
            } else if (compilation is AbstractKotlinNativeCompilation) {
                launch {
                    compilation.configureStdlibAndPlatformDependencies(
                        stdlib,
                        AbstractKotlinNativeCompilation::retrievePlatformDependenciesWithNativeDownloadTask
                    )
                }
            }
        }
    }

    launch { kotlin.excludeStdlibFromNativeSourceSetDependencies() }
}

internal suspend fun AbstractKotlinNativeCompilation.retrievePlatformDependenciesWithNativeDownloadTask(): FileCollection {
    val commonizerTarget = commonizerTarget.await() ?: return project.files()
    val nativeDependency = project.getNativeDistributionDependenciesWithNativeDownloadTask(
        commonizerTarget,
    )
    return nativeDependency
}

internal suspend fun AbstractKotlinNativeCompilation.retrievePlatformDependenciesWithNativeDistribution(): FileCollection {
    val commonizerTarget = commonizerTarget.await() ?: return project.files()
    val nativeBundleBuildService = KotlinNativeBundleBuildService.registerIfAbsent(project)
    val nativeDependency = KotlinNativeBundleBuildService.getNativeDistributionDependencies(
        project,
        commonizerTarget,
        nativeBundleBuildService
    )
    return nativeDependency
}

private suspend fun AbstractKotlinNativeCompilation.configureStdlibAndPlatformDependencies(
    stdlib: FileCollection,
    retrievePlatformDependencies: suspend AbstractKotlinNativeCompilation.() -> FileCollection,
) {
    val updatedCompileDependencyFiles = project.files().from(
        stdlib,
        retrievePlatformDependencies.invoke(this),
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

internal fun Project.getNativeDistributionDependencies(target: CommonizerTarget, getOriginalPlatformLibrariesForTarget: Project.(LeafCommonizerTarget) -> FileCollection): FileCollection {
    return when (target) {
        is LeafCommonizerTarget -> project.getOriginalPlatformLibrariesForTarget(target)
        is SharedCommonizerTarget -> {
            val klibs = project.commonizedNativeDistributionKlibsOrNull(target) ?: return objects.fileCollection()
            objects.fileCollection().from(klibs)
        }
    }
}

internal fun Project.getNativeDistributionDependenciesWithNativeDownloadTask(target: CommonizerTarget): FileCollection {
    return getNativeDistributionDependencies(target) { leafCommonizerTarget ->
        getOriginalPlatformLibrariesForTargetWithNativeDownloadTask(
            leafCommonizerTarget.konanTarget
        )
    }
}

internal fun Project.getNativeDistributionDependenciesWithNativeDistributionProvider(
    konanDistribution: Provider<KonanDistribution>,
    target: CommonizerTarget,
): FileCollection {
    return getNativeDistributionDependencies(target) { leafCommonizerTarget ->
        objects.getOriginalPlatformLibrariesForTargetWithKonanDistribution(
            konanDistribution,
            leafCommonizerTarget
        )
    }
}

private fun ObjectFactory.getOriginalPlatformLibrariesForTargetWithKonanDistribution(
    konanDistribution: Provider<KonanDistribution>,
    target: LeafCommonizerTarget,
): FileCollection =
    getOriginalPlatformLibrariesForTargetWithKonanDistribution(konanDistribution, target.konanTarget)


internal fun Project.getOriginalPlatformLibrariesForTargetWithNativeDownloadTask(
    konanTarget: KonanTarget,
): FileCollection {
    if (project.kotlinPropertiesProvider.isFunctionalTestMode) return project.files().from("nativeDependencies")

    val kotlinNativeDownloadTask = downloadKotlinNativeDistributionTask
    return objects.fileCollection()
        .from(
            kotlinNativeDownloadTask.map { it.getPlatformDependencies(konanTarget.name) },
        )
}

internal fun ObjectFactory.getOriginalPlatformLibrariesForTargetWithKonanDistribution(
    konanDistribution: Provider<KonanDistribution>,
    konanTarget: KonanTarget,
): FileCollection =
    fileCollection()
        .from(
            konanDistribution.map {
                it.platformLibsDir.resolve(konanTarget.name).listLibraryFiles().toSet()
            }
        )


fun File.listLibraryFiles(): List<File> = listFiles().orEmpty()
    .filter { it.isDirectory || it.extension == "klib" }
