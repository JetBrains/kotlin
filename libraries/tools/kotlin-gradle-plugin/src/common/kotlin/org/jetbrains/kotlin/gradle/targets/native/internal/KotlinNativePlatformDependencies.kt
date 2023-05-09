/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.PLATFORM_INTEGERS_SUPPORT_LIBRARY
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.findMetadataCompilation
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.utils.filesProvider
import java.io.File

internal fun Project.setupKotlinNativePlatformDependencies() {
    val kotlin = multiplatformExtensionOrNull ?: return

    if (isAllowCommonizer()) {
        checkNotNull(commonizeNativeDistributionTask) { "Missing commonizeNativeDistributionTask" }
    }

    kotlin.sourceSets.all { sourceSet ->
        launch {
            val target = sourceSet.internal.commonizerTarget.await() ?: return@launch
            addDependencies(sourceSet, getNativeDistributionDependencies(target))
            addDependencies(
                sourceSet, project.filesProvider { setOf(konanDistribution.stdlib) },
                /*
            Shared Native compilations already implicitly add this dependency.
            Adding it again will result in a warning
            */
                isCompilationDependency = false
            )
        }
    }

    launch {
        if (isPlatformIntegerCommonizationEnabled) {
            kotlin.nativeRootSourceSets().forEach { sourceSet ->
                dependencies.add(
                    sourceSet.implementationConfigurationName,
                    "$KOTLIN_MODULE_GROUP:$PLATFORM_INTEGERS_SUPPORT_LIBRARY:${getKotlinPluginVersion()}"
                )
            }
        }
    }
}

internal fun Project.getNativeDistributionDependencies(target: CommonizerTarget): FileCollection {
    return when (target) {
        is LeafCommonizerTarget -> getOriginalPlatformLibrariesFor(target)
        is SharedCommonizerTarget -> commonizeNativeDistributionTask?.get()?.getCommonizedPlatformLibrariesFor(target) ?: project.files()
    }
}

internal suspend fun KotlinMultiplatformExtension.nativeRootSourceSets(): Collection<KotlinSourceSet> {
    val nativeSourceSets = sourceSets.filter { sourceSet -> sourceSet.internal.commonizerTarget.await() != null }
    return nativeSourceSets.filter { sourceSet ->
        val allVisibleSourceSets = sourceSet.dependsOn + getVisibleSourceSetsFromAssociateCompilations(sourceSet)
        allVisibleSourceSets.none { dependency ->
            dependency in nativeSourceSets
        }
    }
}

private fun Project.getOriginalPlatformLibrariesFor(target: LeafCommonizerTarget): FileCollection = project.filesProvider {
    konanDistribution.platformLibsDir.resolve(target.konanTarget.name).listLibraryFiles().toSet()
}

private fun NativeDistributionCommonizerTask.getCommonizedPlatformLibrariesFor(target: SharedCommonizerTarget): FileCollection {
    val targetOutputDirectory = CommonizerOutputFileLayout.resolveCommonizedDirectory(rootOutputDirectory, target)
    return project.filesProvider { targetOutputDirectory.listLibraryFiles() }.builtBy(this)
}

private suspend fun Project.addDependencies(
    sourceSet: KotlinSourceSet, libraries: FileCollection, isCompilationDependency: Boolean = true, isIdeDependency: Boolean = true
) {
    if (isCompilationDependency) {
        findMetadataCompilation(sourceSet)?.let { compilation ->
            compilation.compileDependencyFiles += libraries
        }
    }

    if (isIdeDependency && sourceSet is DefaultKotlinSourceSet) {
        val metadataConfigurationName =
            if (project.isIntransitiveMetadataConfigurationEnabled) sourceSet.intransitiveMetadataConfigurationName
            else sourceSet.implementationMetadataConfigurationName
        dependencies.add(metadataConfigurationName, libraries)
    }
}

internal val Project.konanDistribution: KonanDistribution
    get() = KonanDistribution(project.file(konanHome))

private fun File.listLibraryFiles(): List<File> = listFiles().orEmpty()
    .filter { it.isDirectory || it.extension == "klib" }


internal val Project.isNativeDependencyPropagationEnabled: Boolean
    get() = PropertiesProvider(this).nativeDependencyPropagation ?: true

/**
 * Function signature needs to be kept stable since this is used during import
 * in IDEs (KotlinCommonizerModelBuilder) < 222
 *
 * IDEs >= will use the [ideaImportDependsOn] infrastructure
 */
@JvmName("isAllowCommonizer")
internal fun Project.isAllowCommonizer(): Boolean {
    assert(state.executed) { "'isAllowCommonizer' can only be called after project evaluation" }
    multiplatformExtensionOrNull ?: return false

    return multiplatformExtension.targets.any { it.platformType == KotlinPlatformType.native }
            && isKotlinGranularMetadataEnabled
            && !isNativeDependencyPropagationEnabled // temporary fix: turn on commonizer only when native deps propagation is disabled
}
