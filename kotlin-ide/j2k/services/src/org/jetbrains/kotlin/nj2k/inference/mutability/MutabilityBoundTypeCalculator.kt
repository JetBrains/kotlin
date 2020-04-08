/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.mutability

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.nj2k.inference.common.BoundType
import org.jetbrains.kotlin.nj2k.inference.common.BoundTypeCalculatorImpl
import org.jetbrains.kotlin.nj2k.inference.common.BoundTypeEnhancer
import org.jetbrains.kotlin.nj2k.inference.common.InferenceContext
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class MutabilityBoundTypeCalculator(
    private val resolutionFacade: ResolutionFacade,
    enhancer: BoundTypeEnhancer
) : BoundTypeCalculatorImpl(resolutionFacade, enhancer) {

    private val callResolver = CallResolver(PRESERVING_MUTABILITY_FQ_NAMES)

    override fun interceptCalculateBoundType(inferenceContext: InferenceContext, expression: KtExpression): BoundType? {
        return when (expression) {
            is KtQualifiedExpression -> {
                val selector = expression.selectorExpression ?: return null
                if (callResolver.isNeededCall(selector, resolutionFacade))
                    expression.receiverExpression.boundType(inferenceContext)
                else null
            }
            else -> null
        }
    }

    companion object {
        private val PRESERVING_MUTABILITY_FQ_NAMES = setOf(
            FQ_NAMES.collection.child(Name.identifier("iterator")),
            FQ_NAMES.list.child(Name.identifier("listIterator")),
            FQ_NAMES.map.child(Name.identifier("entries")),
            FQ_NAMES.map.child(Name.identifier("values"))
        )
    }
}