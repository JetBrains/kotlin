/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationDetailsImpl

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationDetails
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.AbstractKotlinFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.GradleKpmDependencyFilesHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ofMetadataCompilationDependencies

internal open class MetadataMappedCompilationDetails<T : KotlinCommonOptions>(
    override val target: KotlinMetadataTarget,
    defaultSourceSet: KotlinSourceSet,
    final override val compilationData: AbstractKotlinFragmentMetadataCompilationData<T>
) : AbstractCompilationDetails<T>(defaultSourceSet) {

    @Suppress("UNCHECKED_CAST")
    override val compilation: KotlinCompilation<T>
        get() = target.compilations.getByName(defaultSourceSet.name) as KotlinCompilation<T>

    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder =
        GradleKpmDependencyFilesHolder.ofMetadataCompilationDependencies(compilationData)

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = compilationData.fragment

    override fun associateWith(other: CompilationDetails<*>) {
        throw UnsupportedOperationException("not supported in the mapped model")
    }

    override val associateCompilations: Set<CompilationDetails<*>>
        get() = emptySet()

    override fun whenSourceSetAdded(sourceSet: KotlinSourceSet) {
        if (sourceSet != defaultSourceSet)
            throw UnsupportedOperationException("metadata compilations have predefined sources")
    }
}