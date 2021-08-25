/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx

data class FragmentDependency(
    val module: ModuleId,
    val fragments: Set<FragmentId>
)

class KpmDependencyExpansion(
    private val module: KotlinModule,
    private val variantMatcher: VariantMatcher
) {
    data class MatchedVariant(
        val module: KotlinModule,
        val variantId: FragmentId
    )

    interface VariantMatcher {
        /**
         * Imports Kotlin Module for given [moduleDependency]
         */
        fun matchVariants(dependant: FragmentId, moduleDependency: ModuleId): MatchedVariant?
    }

    /**
     * Expands [moduleDependency] from [dependant] fragment into [FragmentDependency]
     */
    fun expandDependencies(dependant: FragmentId, moduleDependency: ModuleId): Set<FragmentId> {
        val variants = module.refiningVariants(dependant)
        val intersectedFragments = mutableMapOf<FragmentId,Int>()

        fun fragmentSeen(fragment: Fragment) {
            val id = fragment.id
            intersectedFragments[id] = intersectedFragments.getOrDefault(id, 0) + 1
        }

        var totalMatchedVariants = 0

        for (variant in variants) {
            val matchedVariant = variantMatcher.matchVariants(variant.id, moduleDependency) ?: continue
            totalMatchedVariants++

            matchedVariant
                .module
                .refinementClosure(matchedVariant.variantId)
                .forEach(::fragmentSeen)
        }

        return intersectedFragments.filterValues { totalVariants -> totalVariants == totalMatchedVariants }.keys
    }
}