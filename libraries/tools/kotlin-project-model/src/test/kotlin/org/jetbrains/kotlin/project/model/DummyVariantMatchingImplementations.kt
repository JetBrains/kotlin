/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

class MatchVariantsByExactAttributes : KotlinModuleVariantResolver {
    override fun getChosenVariant(dependingVariant: KotlinModuleVariant, dependencyModule: KotlinModule): KotlinVariantMatchingResult {
        val candidates = dependencyModule.variants
        return candidates.filter { candidate ->
            candidate.isExported && candidate.variantAttributes.all { (attributeKey, candidateValue) ->
                attributeKey !in dependingVariant.variantAttributes.keys ||
                        candidateValue == dependingVariant.variantAttributes.getValue(attributeKey)
            }
        }.let { KotlinVariantMatchingResult.fromMatchingVariants(dependingVariant, dependencyModule, it) }
    }
}

class AssociateVariants : DefaultInternalDependencyExpansion.ContainingModuleVariantResolver {
    override fun getChosenVariant(
        dependingVariant: KotlinModuleVariant,
        candidateVariants: Iterable<KotlinModuleVariant>
    ): KotlinVariantMatchingResult {
        val result = candidateVariants.filter { it in dependingVariant.declaredContainingModuleFragmentDependencies }
        return KotlinVariantMatchingResult.fromMatchingVariants(dependingVariant, dependingVariant.containingModule, result)
    }
}