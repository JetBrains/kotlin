/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface ModuleVariantResolver {
    /**
     * Find the matching module variant of the [dependencyModule] for the consumer's [requestingVariant].
     *
     * This is a partial function. A particular resolver may not be capable to do variant matching for some [dependencyModule] or
     * some [requestingVariant] (to such an extent that a resolver may only know how to resolve a single [dependencyModule]'s variants, or
     * how to resolve module variants for a particular [requestingVariant]).
     * In this case it should return [VariantResolution.Unknown], and the caller (which might be an aggregating [ModuleVariantResolver])
     * may consult other sources to get the variant match.
     */
    fun getChosenVariant(requestingVariant: KotlinModuleVariant, dependencyModule: KotlinModule): VariantResolution
}

/**
 * Represents the results of [dependencyModule]'s variant resolution for the [requestingVariant],
 * usually done by some [ModuleVariantResolver].
 */
sealed class VariantResolution(
    val requestingVariant: KotlinModuleVariant,
    val dependencyModule: KotlinModule
) {
    companion object {
        fun fromMatchingVariants(
            requestingVariant: KotlinModuleVariant,
            dependencyModule: KotlinModule,
            matchingVariants: Collection<KotlinModuleVariant>
        ) = when (matchingVariants.size) {
            0 -> NoVariantMatch(requestingVariant, dependencyModule)
            1 -> VariantMatch(requestingVariant, dependencyModule, matchingVariants.single())
            else -> AmbiguousVariants(requestingVariant, dependencyModule, matchingVariants)
        }
    }

    override fun toString(): String = when (this) {
        is VariantMatch -> "match: ${chosenVariant.fragmentName}"
        is Unknown -> "unknown"
        is NotRequested -> "not requested"
        is NoVariantMatch -> "no match"
        is AmbiguousVariants -> "ambiguity: ${matchingVariants.joinToString { it.fragmentName }}"
    }

    /**
     * The resolver decided that the [chosenVariant] is the best variant match.
     */
    class VariantMatch(
        requestingVariant: KotlinModuleVariant,
        dependencyModule: KotlinModule,
        val chosenVariant: KotlinModuleVariant
    ) : VariantResolution(requestingVariant, dependencyModule)

    class Unknown(requestingVariant: KotlinModuleVariant, dependencyModule: KotlinModule) :
        VariantResolution(requestingVariant, dependencyModule)

    /**
     * Returned when the resolver detects that the [requestingVariant] does not depend on [dependencyModule] and therefore should not get
     * any variant of that module at all.
     */
    class NotRequested(requestingVariant: KotlinModuleVariant, dependencyModule: KotlinModule) :
        VariantResolution(requestingVariant, dependencyModule)

    /**
     * Returned when the resolver could not find any matching of the [dependencyModule] for the [requestingVariant], or variant matching was
     * done externally and the external system did not provide any details of the failure.
     */
    class NoVariantMatch(
        requestingVariant: KotlinModuleVariant,
        dependencyModule: KotlinModule
    ) : VariantResolution(requestingVariant, dependencyModule)

    /**
     * Returned when the resolver found multiple matching variants in the [dependencyModule] and failed to choose one of them as the
     * best match for the [requestingVariant].
     */
    class AmbiguousVariants(
        requestingVariant: KotlinModuleVariant,
        dependencyModule: KotlinModule,
        val matchingVariants: Iterable<KotlinModuleVariant>
    ) : VariantResolution(requestingVariant, dependencyModule)
}