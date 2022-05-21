/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.gdt

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil


/**
 * Core algorithm that expands fragment to module dependency into
 * granular fragment to fragment dependency with respect of symbols visibility
 */
class DependencyExpander<Fragment, Variant, DependencyFragment, DependencyVariant, ModuleDependency>(
    val participatesInVariants: Fragment.() -> Set<Variant>,
    val matchVariant: Variant.(dependency: ModuleDependency) -> DependencyVariant,
    val fragmentsOfVariant: DependencyVariant.() -> Set<DependencyFragment>,
) {
    sealed class DependencyExpansionResult<DependencyFragment> {
        data class Success<DependencyFragment>(
            val fragments: Set<DependencyFragment>
        ) : DependencyExpansionResult<DependencyFragment>()
    }

    fun expand(fragment: Fragment, dependencyRequest: ModuleDependency): DependencyExpansionResult<DependencyFragment> {
        // Step 1. Match variants
        val consumerVariants = fragment.participatesInVariants()
        val dependencyVariants = consumerVariants.map { it.matchVariant(dependencyRequest) }

        // Step 2. Extract & Intersect fragments
        val dependencyFragments = dependencyVariants.map { it.fragmentsOfVariant() }
        val intersection = dependencyFragments.reduce { acc, fragments -> acc.intersect(fragments) }

        return DependencyExpansionResult.Success(fragments = intersection)
    }
}

//fun GradleTCSDependencyExpander(project: Project) = DependencyExpander<
//        KotlinSourceSet,
//        KotlinCompilation<*>,
//        Nothing,
//        Nothing,
//        Nothing> (
//    participatesInVariants = { CompilationSourceSetUtil.compilationsBySourceSets(project)[this] ?: emptySet() },
//    matchVariant = {  },
//    fragmentsOfVariant = { TODO() }
//)

class GradleVariantMatcher(
    val variantEndConfigurations: Map<String, Configuration>
) {
    // fun matchVariant(dependency: Dependency)
}

class GradleGranularDependencyTransformation(
    val project: Project
) {
    fun transform(configurationToResolve: Configuration) {
        // Step 0: Resolve configuration
        val resolutionResult = configurationToResolve.incoming.resolutionResult

        // Step 1: Get all MPP dependencies
        val allModuleDependencies = resolutionResult.allDependencies.filterIsInstance<ResolvedDependencyResult>()
        val mppDependencies = allModuleDependencies.filter { it.isMppDependency }

        // Step 2: For each MPP depdendency apply transformation
        val granularDependencies = mppDependencies.associateBy { dependency -> visibleSourceSetsOf(dependency) }
    }

    private fun visibleSourceSetsOf(dependency: ResolvedDependencyResult) {
        TODO("Not yet implemented")
    }

    private val ResolvedDependencyResult.isMppDependency: Boolean
        get() {
            TODO("Not yet implemented")
        }
}