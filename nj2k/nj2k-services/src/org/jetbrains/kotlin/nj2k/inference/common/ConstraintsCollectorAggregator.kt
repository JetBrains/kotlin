/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.nj2k.inference.common.collectors.ConstraintsCollector
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class ConstraintsCollectorAggregator(
    private val resolutionFacade: ResolutionFacade,
    private val collectors: List<ConstraintsCollector>
) {
    fun collectConstraints(
        boundTypeCalculator: BoundTypeCalculator,
        inferenceContext: InferenceContext,
        elements: List<KtElement>
    ): List<Constraint> {
        val constraintsBuilder = ConstraintBuilder(inferenceContext, boundTypeCalculator)
        for (element in elements) {
            element.forEachDescendantOfType<KtElement> { innerElement ->
                if (innerElement.getStrictParentOfType<KtImportDirective>() != null) return@forEachDescendantOfType
                for (collector in collectors) {
                    with(collector) {
                        constraintsBuilder.collectConstraints(
                            innerElement,
                            boundTypeCalculator,
                            inferenceContext,
                            resolutionFacade
                        )
                    }
                }
            }
        }
        return constraintsBuilder.collectedConstraints
    }
}
