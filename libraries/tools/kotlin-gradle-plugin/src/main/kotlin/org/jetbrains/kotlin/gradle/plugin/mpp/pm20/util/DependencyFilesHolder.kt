/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.AbstractKotlinFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariantWithRuntime
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.resolvableMetadataConfigurationName

interface DependencyFilesHolder {
    val dependencyConfigurationName: String
    var dependencyFiles: FileCollection

    companion object
}

internal fun DependencyFilesHolder.Companion.ofVariantCompileDependencies(variant: KotlinGradleVariant): DependencyFilesHolder =
    object : DependencyFilesHolder {
        override val dependencyConfigurationName: String
            get() = variant.compileDependenciesConfiguration.name
        override var dependencyFiles: FileCollection
            get() = variant.compileDependencyFiles
            set(value) { variant.compileDependencyFiles = value }
    }

internal fun DependencyFilesHolder.Companion.ofVariantRuntimeDependencies(variant: KotlinGradleVariantWithRuntime): DependencyFilesHolder =
    object : DependencyFilesHolder {
        override val dependencyConfigurationName: String
            get() = variant.runtimeDependenciesConfiguration.name
        override var dependencyFiles: FileCollection
            get() = variant.runtimeDependencyFiles
            set(value) { variant.runtimeDependencyFiles = value }
    }

internal fun DependencyFilesHolder.Companion.ofMetadataCompilationDependencies(
    compilationData: AbstractKotlinFragmentMetadataCompilationData<*>
) = object : DependencyFilesHolder {
    override val dependencyConfigurationName: String
        get() = compilationData.fragment.containingModule.resolvableMetadataConfigurationName

    override var dependencyFiles: FileCollection
        get() = compilationData.compileDependencyFiles
        set(value) { compilationData.compileDependencyFiles = value }
}

class SimpleDependencyFilesHolder(
    override val dependencyConfigurationName: String,
    override var dependencyFiles: FileCollection
) : DependencyFilesHolder

internal fun Project.newDependencyFilesHolder(dependencyConfigurationName: String): DependencyFilesHolder =
    SimpleDependencyFilesHolder(dependencyConfigurationName, project.files())