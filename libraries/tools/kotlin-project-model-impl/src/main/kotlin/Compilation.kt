/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx

/**
 * Compiler settings — the set of data which completely defines the compiler behaviour
 * Semantically, it consists of frontend settings (which define how the code is analyzed)
 * and backend settings (define how the resulting binary is compiled/linked)
 *
 * See [Design doc](https://helpserver.labs.jb.gg/help/kotlin-project-model/core-notions.html#compiler-settings)
 */
data class FragmentCompilerSettings(
    /**
     * Only sources of [fragment] should be compiled
     */
    val fragment: Fragment,

    /**
     * In design doc there is a mention of
     *
     * _set of targets for which this module fragment will be compiled_
     * _Examples: {JVM 1.8, JS}, {Android, iOSx64, iOS ARM 64}_
     *
     * But I guess it is logical to have whole inferred attributes in there
     * See [Module fragment paragraph in design doc](https://helpserver.labs.jb.gg/help/kotlin-project-model/core-notions.html#88d669e5)
     */
    val attributes: Map<Attribute.Key, Attribute>,

    /**
     * KLIBs of [refinements] should be included as friend modules in compilation classpath
     * It is important that metadata KLIBs of these [refinements] already compiled
     */
    val refinements: Set<FragmentId>,

    /**
     * Expanded [FragmentDependency]'s that must be included into compilation classpath
     */
    val dependencies: Set<FragmentDependency>
)

/**
 * Compiler settings — the set of data which completely defines the compiler behaviour
 * Semantically, it consists of frontend settings (which define how the code is analyzed)
 * and backend settings (define how the resulting binary is compiled/linked)
 *
 * See [Design doc](https://helpserver.labs.jb.gg/help/kotlin-project-model/core-notions.html#compiler-settings)
 */
data class VariantCompilerSettings(
    /**
     * Platform can be used to identify which concrete compiler (JVM, JS, Native) should be called
     */
    val variant: Variant,

    /**
     * Set of [FragmentId] that considered as "common" an
     * i.e. should be passed into cli's compiler argument -Xcommon-sources
     */
    val refinements: Set<FragmentId>,

    /**
     * Expanded [FragmentDependency]'s that must be included into compilation classpath
     */
    val dependencies: Set<FragmentDependency>
) {
    val attributes get() = variant.attributes
}

/**
 * TODO: Find better name
 *
 * This class should lay in the KPM domain only and represent the "pure KPM compilation logic"
 * i.e. answer such question:
 * What are compilation settings if I want to compile a fragment or variant of given Kotlin Module?
 */
class KpmCompilationProcessor(
    private val module: KotlinModule,
    private val dependencyExpansion: KpmDependencyExpansion
) {
    /**
     * Returns [null] when Fragment cannot be compiled into Metadata
     */
    fun metadataCompilation(fragmentId: FragmentId): FragmentCompilerSettings? {
        val fragment = module.fragments[fragmentId] ?: error("Fragment not found: $fragmentId")
        if (!fragment.isMetadataCompilationSupported()) return null

        val refinementClosure = module.refinementClosure(fragmentId, includeStart = false)
        val moduleDependencies = fragment.moduleDependencies + refinementClosure.flatMap { it.moduleDependencies }.toSet()

        val dependencies = moduleDependencies.map { dependencyModule ->
            FragmentDependency(
                module = dependencyModule,
                fragments = dependencyExpansion.expandDependencies(fragment.id, dependencyModule)
            )
        }.toSet()

        return FragmentCompilerSettings(
            fragment = fragment,
            refinements = refinementClosure.map(Fragment::id).toSet(),
            dependencies = dependencies,
            attributes = module.fragmentAttributes(fragmentId)
        )
    }

    fun compileVariant(variantId: FragmentId): VariantCompilerSettings {
        val variant = module.fragments[variantId] ?: error("Variant not found")
        if (variant !is Variant) error("Fragment with id $variantId is not Variant")
        val refinementClosure = module.refinementClosure(variantId, includeStart = false)

        val refinements = refinementClosure.map { it.id }.toSet()
        val sources = refinements + variantId

        // TODO: there can be duplicates, squash them by fragmentId
        val moduleDependencies = variant.moduleDependencies + refinementClosure.flatMap { it.moduleDependencies }.toSet()
        val dependencies = moduleDependencies.mapNotNull { dependencyModule ->
            // We expect to see only single variant dependency
            val fragmentDependencies = dependencyExpansion.expandDependencies(variant.id, dependencyModule)
            // If nothing found, this module dependency isn't applicable
            if (fragmentDependencies.isEmpty()) return@mapNotNull null

            FragmentDependency(
                module = dependencyModule,
                fragments = fragmentDependencies
            )
        }.toSet()

        return VariantCompilerSettings(
            variant = variant,
            refinements = refinements,
            dependencies = dependencies
        )
    }

    fun isMetadataCompilationSupported(fragmentId: FragmentId): Boolean {
        val fragment = module.fragments[fragmentId] ?: error("Fragment not found: $fragmentId")
        return fragment.isMetadataCompilationSupported()
    }

    private fun Fragment.isMetadataCompilationSupported(): Boolean = when {
        this is Variant && platform == Platform.Native -> true
        module.fragmentPlatforms(id).size > 1 -> true
        else -> false
    }
}