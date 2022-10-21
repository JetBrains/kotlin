/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx

/**
 * API surface of KPM for Build System environment.
 * It is responsible for Dependency Resolution, providing source files and other unrelated to KPM information
 * This adapter is considered to be aware of context of some [KotlinModule]
 */
interface KpmBuildSystemAdapter {
    /**
     * The [KotlinModule] for which the given instance of [KpmBuildSystemAdapter] is configured
     */
    val module: KotlinModule

    /**
     *  Some BuildSystems can provide its own VariantMatching functionalities.
     *  They have to respect Kotlin [Attribute] relations anyway.
     *
     *  By default [DefaultVariantMatcher] is used
     */
    val variantMatcher: KpmDependencyExpansion.VariantMatcher get() = DefaultVariantMatcher(module, this::dependencyModule)

    /**
     * Build system adapter should be able to locate and extract dependency [KotlinModule] by its [moduleId]
     *
     * Invariants:
     *  * [moduleId] must be present in [Fragment.moduleDependencies] of at least one [Fragment] from [KotlinModule.fragments]
     */
    fun dependencyModule(moduleId: ModuleId): KotlinModule

    /**
     * Returns source paths of a fragment
     */
    fun fragmentSources(fragmentId: FragmentId): List<String>

    /**
     * Returns classpath to metadata compilation output of given [fragmentId]
     */
    fun fragmentMetadataClasspath(fragmentId: FragmentId): String

    /**
     * Returns classpath to metadata classes of external [fragmentDependency]
     */
    fun fragmentDependencyMetadataArtifacts(fragmentDependency: FragmentDependency): List<String>

    /**
     * Returns classpath to complete variant artifact and its complete dependencies
     */
    fun variantDependencyArtifacts(fragmentDependency: FragmentDependency): List<String>
}