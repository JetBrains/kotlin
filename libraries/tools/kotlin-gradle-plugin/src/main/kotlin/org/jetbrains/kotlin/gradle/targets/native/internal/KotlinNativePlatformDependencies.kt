/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.internal.MissingNativeStdlibWarning.showMissingNativeStdlibWarning
import org.jetbrains.kotlin.gradle.utils.filesProvider
import java.io.File

internal fun Project.setupKotlinNativePlatformDependencies() {
    val kotlin = multiplatformExtensionOrNull ?: return

    if (!konanDistribution.stdlib.exists()) {
        showMissingNativeStdlibWarning()
    }

    if (isAllowCommonizer()) {
        checkNotNull(commonizeNativeDistributionTask) { "Missing commonizeNativeDistributionTask" }
    }

    kotlin.sourceSets.forEach { sourceSet ->
        val target = getCommonizerTarget(sourceSet) ?: return@forEach
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

internal fun Project.getNativeDistributionDependencies(target: CommonizerTarget): FileCollection {
    return when (target) {
        is LeafCommonizerTarget -> getOriginalPlatformLibrariesFor(target)
        is SharedCommonizerTarget -> commonizeNativeDistributionTask?.get()?.getCommonizedPlatformLibrariesFor(target) ?: project.files()
    }
}

private fun Project.getOriginalPlatformLibrariesFor(target: LeafCommonizerTarget): FileCollection = project.filesProvider {
    konanDistribution.platformLibsDir.resolve(target.konanTarget.name).listLibraryFiles().toSet()
}

private fun NativeDistributionCommonizerTask.getCommonizedPlatformLibrariesFor(target: SharedCommonizerTarget): FileCollection {
    val targetOutputDirectory = CommonizerOutputFileLayout.resolveCommonizedDirectory(getRootOutputDirectory(), target)
    return project.filesProvider { targetOutputDirectory.listLibraryFiles() }.builtBy(this)
}

private fun Project.addDependencies(
    sourceSet: KotlinSourceSet, libraries: FileCollection, isCompilationDependency: Boolean = true, isIdeDependency: Boolean = true
) {
    if (isCompilationDependency) {
        getMetadataCompilationForSourceSet(sourceSet)?.let { compilation ->
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

private val Project.konanDistribution: KonanDistribution
    get() = KonanDistribution(project.file(konanHome))

private fun File.listLibraryFiles(): List<File> = listFiles().orEmpty()
    .filter { it.isDirectory || it.extension == "klib" }


internal val Project.isNativeDependencyPropagationEnabled: Boolean
    get() = PropertiesProvider(this).nativeDependencyPropagation ?: true

//for reflection call from KotlinCommonizerModelBuilder
// DO NOT REFACTOR THIS FUNCTION!
//  TODO SELLMAIR: Resolve fragile reflection call from IDE plugin
@JvmOverloads
@JvmName("isAllowCommonizer")
internal fun Project.isAllowCommonizer(
    kotlinVersion: String = getKotlinPluginVersion()
): Boolean {
    assert(state.executed) { "'isAllowCommonizer' can only be called after project evaluation" }
    multiplatformExtensionOrNull ?: return false

    //register commonizer only for 1.4+, only for HMPP projects
    return compareVersionNumbers(kotlinVersion, "1.4") >= 0
            && multiplatformExtension.targets.any { it.platformType == KotlinPlatformType.native }
            && isKotlinGranularMetadataEnabled
            && !isNativeDependencyPropagationEnabled // temporary fix: turn on commonizer only when native deps propagation is disabled
}
