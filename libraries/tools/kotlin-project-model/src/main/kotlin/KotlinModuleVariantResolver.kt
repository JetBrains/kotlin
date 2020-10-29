/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface KotlinModuleVariantResolver {
    fun getChosenVariant(dependingVariant: KotlinModuleVariant, dependencyModule: KotlinModule): KotlinVariantMatchingResult
}

sealed class KotlinVariantMatchingResult(
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
}

class VariantMatch(
    requestingVariant: KotlinModuleVariant,
    dependencyModule: KotlinModule,
    val chosenVariant: KotlinModuleVariant
) : KotlinVariantMatchingResult(requestingVariant, dependencyModule)

class NoVariantMatch(
    requestingVariant: KotlinModuleVariant,
    dependencyModule: KotlinModule
) : KotlinVariantMatchingResult(requestingVariant, dependencyModule)

class AmbiguousVariants(
    requestingVariant: KotlinModuleVariant,
    dependencyModule: KotlinModule,
    val matchingVariants: Iterable<KotlinModuleVariant>
) : KotlinVariantMatchingResult(requestingVariant, dependencyModule)
