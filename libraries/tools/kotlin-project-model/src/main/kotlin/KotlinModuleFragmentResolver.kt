/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment

interface KotlinModuleFragmentResolver {
    fun getChosenFragments(dependingFragment: KotlinModuleFragment, dependencyModule: KotlinModule): KotlinChosenFragments
}

class KotlinChosenFragments(
    val module: KotlinModule,
    val chosenFragments: Iterable<KotlinModuleFragment>,
    val variantMatchingResults: Iterable<KotlinVariantMatchingResult>
)

class DefaultKotlinModuleFragmentResolver(
    private val variantResolver: KotlinModuleVariantResolver
) : KotlinModuleFragmentResolver {
    override fun getChosenFragments(dependingFragment: KotlinModuleFragment, dependencyModule: KotlinModule): KotlinChosenFragments {
        val dependingModule = dependingFragment.containingModule
        val containingVariants = dependingModule.variantsContainingFragment(dependingFragment)

        val chosenVariants = containingVariants.map { variantResolver.getChosenVariant(it, dependencyModule) }

        val chosenFragments = chosenVariants.map { variantResolution ->
            when (variantResolution) {
                is VariantMatch -> variantResolution.chosenVariant.refinesClosure
                else -> emptySet()
            }
        }

        val result = if (chosenFragments.isEmpty())
            emptyList<KotlinModuleFragment>()
        else chosenFragments
            // Note this emulates the existing behavior that is lenient wrt to unresolved modules, but gives imprecise results. TODO revisit
            .filter { it.isNotEmpty() }
            .reduce { acc, it -> acc.intersect(it) }

        return KotlinChosenFragments(dependencyModule, result, chosenVariants)
    }
}