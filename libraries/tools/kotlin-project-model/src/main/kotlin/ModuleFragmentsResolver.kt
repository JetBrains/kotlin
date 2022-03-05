/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment

interface ModuleFragmentsResolver {
    fun getChosenFragments(
        requestingFragment: KotlinModuleFragment,
        dependencyModule: KotlinModule
    ): FragmentResolution
}

sealed class FragmentResolution(val requestingFragment: KotlinModuleFragment, val dependencyModule: KotlinModule) {
    class ChosenFragments(
        requestingFragment: KotlinModuleFragment,
        dependencyModule: KotlinModule,
        val visibleFragments: Iterable<KotlinModuleFragment>,
        val variantResolutions: Iterable<VariantResolution>
    ) : FragmentResolution(requestingFragment, dependencyModule)

    class NotRequested(requestingFragment: KotlinModuleFragment, dependencyModule: KotlinModule) :
        FragmentResolution(requestingFragment, dependencyModule)

    // TODO: think about restricting calls with the type system to avoid partial functions in resolvers?
    class Unknown(requestingFragment: KotlinModuleFragment, dependencyModule: KotlinModule) :
        FragmentResolution(requestingFragment, dependencyModule)
}

class DefaultModuleFragmentsResolver(
    private val variantResolver: ModuleVariantResolver
) : ModuleFragmentsResolver {
    override fun getChosenFragments(
        requestingFragment: KotlinModuleFragment,
        dependencyModule: KotlinModule
    ): FragmentResolution {
        val dependingModule = requestingFragment.containingModule
        val containingVariants = dependingModule.variantsContainingFragment(requestingFragment)

        val chosenVariants = containingVariants.map { variantResolver.getChosenVariant(it, dependencyModule) }

        // TODO: extend this to more cases with non-matching variants, revisit the behavior when no matching variant is found once we fix
        //       local publishing of libraries with missing host-specific parts (it breaks transitive dependencies now)
        if (chosenVariants.none { it is VariantResolution.VariantMatch })
            return FragmentResolution.NotRequested(requestingFragment, dependencyModule)

        val chosenFragments = chosenVariants.map { variantResolution ->
            when (variantResolution) {
                is VariantResolution.VariantMatch -> variantResolution.chosenVariant.withRefinesClosure
                else -> emptySet()
            }
        }

        val result = if (chosenFragments.isEmpty())
            emptyList<KotlinModuleFragment>()
        else chosenFragments
            // Note this emulates the existing behavior that is lenient wrt to unresolved modules, but gives imprecise results. TODO revisit
            .filter { it.isNotEmpty() }
            .reduce { acc, it -> acc.intersect(it) }

        return FragmentResolution.ChosenFragments(requestingFragment, dependencyModule, result, chosenVariants)
    }
}
