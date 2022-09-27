/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationDetailsImpl

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationDetails
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationDetailsWithRuntime
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmVariantInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmVariantWithRuntimeInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.GradleKpmDependencyFilesHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ofVariantCompileDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ofVariantRuntimeDependencies
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSet

internal open class VariantMappedCompilationDetails<T : KotlinCommonOptions>(
    open val variant: GradleKpmVariantInternal,
    override val target: KotlinTarget,
    defaultSourceSet: FragmentMappedKotlinSourceSet,
) : AbstractCompilationDetails<T>(defaultSourceSet) {

    @Suppress("UNCHECKED_CAST")
    override val compilationData: KotlinCompilationData<T>
        get() = variant.compilationData as KotlinCompilationData<T>

    override fun whenSourceSetAdded(sourceSet: KotlinSourceSet) {
        compilation.defaultSourceSet.dependsOn(sourceSet)
    }

    override fun associateWith(other: CompilationDetails<*>) {
        if (other !is VariantMappedCompilationDetails<*>)
            error("a mapped variant can't be associated with a legacy one")
        val otherModule = other.variant.containingModule
        if (otherModule === variant.containingModule)
            error("cannot associate $compilation with ${other.compilation} as they are mapped to the same $otherModule")
        variant.containingModule.dependencies { implementation(otherModule) }
    }

    override val associateCompilations: Set<CompilationDetails<*>> get() = emptySet()

    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder
        get() = GradleKpmDependencyFilesHolder.ofVariantCompileDependencies(variant)

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = variant

}

internal open class VariantMappedCompilationDetailsWithRuntime<T : KotlinCommonOptions>(
    override val variant: GradleKpmVariantWithRuntimeInternal,
    target: KotlinTarget,
    defaultSourceSet: FragmentMappedKotlinSourceSet
) : VariantMappedCompilationDetails<T>(variant, target, defaultSourceSet),
    CompilationDetailsWithRuntime<T> {
    override val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder
        get() = GradleKpmDependencyFilesHolder.ofVariantRuntimeDependencies(variant)
}
