/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

class MatchVariantsByExactAttributes : KpmModuleVariantResolver {
    override fun getChosenVariant(requestingVariant: KpmVariant, dependencyModule: KpmModule): KpmVariantResolution {
        val candidates = dependencyModule.variants
        return candidates.filter { candidate ->
            candidate.variantAttributes.all { (attributeKey, candidateValue) ->
                attributeKey !in requestingVariant.variantAttributes.keys ||
                        candidateValue == requestingVariant.variantAttributes.getValue(attributeKey)
            }
        }.let { KpmVariantResolution.fromMatchingVariants(requestingVariant, dependencyModule, it) }
    }
}
