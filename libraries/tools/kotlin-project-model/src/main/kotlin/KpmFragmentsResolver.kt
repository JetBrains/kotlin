/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment

interface KpmFragmentsResolver {
    fun getChosenFragments(
        requestingFragment: KpmFragment,
        dependencyModule: KpmModule
    ): KpmFragmentResolution
}

sealed class KpmFragmentResolution(val requestingFragment: KpmFragment, val dependencyModule: KpmModule) {
    class ChosenFragments(
        requestingFragment: KpmFragment,
        dependencyModule: KpmModule,
        val visibleFragments: Iterable<KpmFragment>,
        val variantResolutions: Iterable<KpmVariantResolution>
    ) : KpmFragmentResolution(requestingFragment, dependencyModule)

    class NotRequested(requestingFragment: KpmFragment, dependencyModule: KpmModule) :
        KpmFragmentResolution(requestingFragment, dependencyModule)

    // TODO: think about restricting calls with the type system to avoid partial functions in resolvers?
    class Unknown(requestingFragment: KpmFragment, dependencyModule: KpmModule) :
        KpmFragmentResolution(requestingFragment, dependencyModule)
}

class KpmDefaultFragmentsResolver(
    private val variantResolver: KpmModuleVariantResolver
) : KpmFragmentsResolver {
    override fun getChosenFragments(
        requestingFragment: KpmFragment,
        dependencyModule: KpmModule
    ): KpmFragmentResolution {
        val dependingModule = requestingFragment.containingModule
        val containingVariants = dependingModule.variantsContainingFragment(requestingFragment)

        val chosenVariants = containingVariants.map { variantResolver.getChosenVariant(it, dependencyModule) }

        // TODO: extend this to more cases with non-matching variants, revisit the behavior when no matching variant is found once we fix
        //       local publishing of libraries with missing host-specific parts (it breaks transitive dependencies now)
        if (chosenVariants.none { it is KpmVariantResolution.KpmVariantMatch })
            return KpmFragmentResolution.NotRequested(requestingFragment, dependencyModule)

        val chosenFragments = chosenVariants.map { variantResolution ->
            when (variantResolution) {
                is KpmVariantResolution.KpmVariantMatch -> variantResolution.chosenVariant.withRefinesClosure
                else -> emptySet()
            }
        }

        val result = if (chosenFragments.isEmpty())
            emptyList<KpmFragment>()
        else chosenFragments
            // Note this emulates the existing behavior that is lenient wrt to unresolved modules, but gives imprecise results. TODO revisit
            .filter { it.isNotEmpty() }
            .reduce { acc, it -> acc.intersect(it) }

        return KpmFragmentResolution.ChosenFragments(requestingFragment, dependencyModule, result, chosenVariants)
    }
}
