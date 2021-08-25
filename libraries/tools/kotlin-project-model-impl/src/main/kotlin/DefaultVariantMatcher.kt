/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx

/**
 * Simply matches through fragment attributes `isCompatible` relation
 *
 * Isn't able to handle multiple matches hence just fail with error
 * Returns null when no variants are matched
 */
class DefaultVariantMatcher(
    val mainModule: KotlinModule,
    val findModule: (ModuleId) -> KotlinModule?
) : KpmDependencyExpansion.VariantMatcher {
    override fun matchVariants(dependant: FragmentId, moduleDependency: ModuleId): KpmDependencyExpansion.MatchedVariant? {
        val moduleDependencyInstance = findModule(moduleDependency) ?: error("Module Not found by id: $moduleDependency")
        val dependantVariant = mainModule.variant(dependant)

        val matchedVariants = moduleDependencyInstance
            .variants
            .filterValues { dependencyVariant -> dependantVariant isCompatible dependencyVariant }
            .values

        if (matchedVariants.isEmpty()) return null
        val matchedVariant = matchedVariants.singleOrNull() ?: error("More than 1 matched variants found for ${dependant} from $moduleDependency")

        return KpmDependencyExpansion.MatchedVariant(moduleDependencyInstance, matchedVariant.id)
    }
}