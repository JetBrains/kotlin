/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles

abstract class AbstractKotlinCompilationToRunnableFiles<T : KotlinCommonOptions>(
    override val compilationDetails: CompilationDetailsWithRuntime<T>,
) : AbstractKotlinCompilation<T>(compilationDetails),
    KotlinCompilationToRunnableFiles<T> {

    final override val runtimeDependencyConfigurationName: String get() = compilationDetails.runtimeDependencyFilesHolder.dependencyConfigurationName
    final override var runtimeDependencyFiles: FileCollection
        get() = compilationDetails.runtimeDependencyFilesHolder.dependencyFiles
        set(value) {
            compilationDetails.runtimeDependencyFilesHolder.dependencyFiles = value
        }

    override val relatedConfigurationNames: List<String>
        get() = super<AbstractKotlinCompilation>.relatedConfigurationNames + runtimeDependencyConfigurationName
}