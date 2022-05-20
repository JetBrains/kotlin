/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface KpmModuleVariantResolver {
    /**
     * Find the matching module variant of the [dependencyModule] for the consumer's [requestingVariant].
     *
     * This is a partial function. A particular resolver may not be capable to do variant matching for some [dependencyModule] or
     * some [requestingVariant] (to such an extent that a resolver may only know how to resolve a single [dependencyModule]'s variants, or
     * how to resolve module variants for a particular [requestingVariant]).
     * In this case it should return [KpmVariantResolution.Unknown], and the caller (which might be an aggregating [KpmModuleVariantResolver])
     * may consult other sources to get the variant match.
     */
    fun getChosenVariant(requestingVariant: KpmVariant, dependencyModule: KpmModule): KpmVariantResolution
}

/**
 * Represents the results of [dependencyModule]'s variant resolution for the [requestingVariant],
 * usually done by some [KpmModuleVariantResolver].
 */
sealed class KpmVariantResolution(
    val requestingVariant: KpmVariant,
    val dependencyModule: KpmModule
) {
    companion object {
        fun fromMatchingVariants(
            requestingVariant: KpmVariant,
            dependencyModule: KpmModule,
            matchingVariants: Collection<KpmVariant>
        ) = when (matchingVariants.size) {
            0 -> KpmNoVariantMatch(requestingVariant, dependencyModule)
            1 -> KpmVariantMatch(requestingVariant, dependencyModule, matchingVariants.single())
            else -> KpmAmbiguousVariants(requestingVariant, dependencyModule, matchingVariants)
        }
    }

    override fun toString(): String = when (this) {
        is KpmVariantMatch -> "match: ${chosenVariant.fragmentName}"
        is Unknown -> "unknown"
        is NotRequested -> "not requested"
        is KpmNoVariantMatch -> "no match"
        is KpmAmbiguousVariants -> "ambiguity: ${matchingVariants.joinToString { it.fragmentName }}"
    }

    /**
     * The resolver decided that the [chosenVariant] is the best variant match.
     */
    class KpmVariantMatch(
        requestingVariant: KpmVariant,
        dependencyModule: KpmModule,
        val chosenVariant: KpmVariant
    ) : KpmVariantResolution(requestingVariant, dependencyModule)

    // TODO: think about restricting calls with the type system to avoid partial functions in resolvers?
    class Unknown(requestingVariant: KpmVariant, dependencyModule: KpmModule) :
        KpmVariantResolution(requestingVariant, dependencyModule)

    /**
     * Returned when the resolver detects that the [requestingVariant] does not depend on [dependencyModule] and therefore should not get
     * any variant of that module at all.
     */
    class NotRequested(requestingVariant: KpmVariant, dependencyModule: KpmModule) :
        KpmVariantResolution(requestingVariant, dependencyModule)

    /**
     * Returned when the resolver could not find any matching of the [dependencyModule] for the [requestingVariant], or variant matching was
     * done externally and the external system did not provide any details of the failure.
     */
    class KpmNoVariantMatch(
        requestingVariant: KpmVariant,
        dependencyModule: KpmModule
    ) : KpmVariantResolution(requestingVariant, dependencyModule)

    /**
     * Returned when the resolver found multiple matching variants in the [dependencyModule] and failed to choose one of them as the
     * best match for the [requestingVariant].
     */
    class KpmAmbiguousVariants(
        requestingVariant: KpmVariant,
        dependencyModule: KpmModule,
        val matchingVariants: Iterable<KpmVariant>
    ) : KpmVariantResolution(requestingVariant, dependencyModule)
}
