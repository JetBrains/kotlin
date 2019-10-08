/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.mutability

import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.nj2k.inference.common.collectors.ConstraintsCollector
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class MutabilityConstraintsCollector : ConstraintsCollector() {
    private val callResolver = CallResolver(MUTATOR_CALL_FQ_NAMES)

    override fun ConstraintBuilder.collectConstraints(
        element: KtElement,
        boundTypeCalculator: BoundTypeCalculator,
        inferenceContext: InferenceContext,
        resolutionFacade: ResolutionFacade
    ) {
        when (element) {
            is KtQualifiedExpression -> {
                val callExpression = element.selectorExpression?.safeAs<KtCallExpression>() ?: return
                if (callResolver.isNeededCall(callExpression, resolutionFacade)) {
                    element.receiverExpression.isTheSameTypeAs(State.LOWER, ConstraintPriority.USE_AS_RECEIVER)
                }
            }
        }
    }

    companion object {
        private val MUTATOR_CALL_FQ_NAMES = setOf(
            FQ_NAMES.mutableCollection.child(Name.identifier("add")),
            FQ_NAMES.mutableCollection.child(Name.identifier("addAll")),
            FQ_NAMES.mutableCollection.child(Name.identifier("remove")),
            FQ_NAMES.mutableCollection.child(Name.identifier("removeAll")),
            FQ_NAMES.mutableCollection.child(Name.identifier("retainAll")),
            FQ_NAMES.mutableCollection.child(Name.identifier("clear")),
            FQ_NAMES.mutableCollection.child(Name.identifier("removeIf")),

            FQ_NAMES.mutableList.child(Name.identifier("addAll")),
            FQ_NAMES.mutableList.child(Name.identifier("set")),

            FQ_NAMES.mutableMap.child(Name.identifier("put")),
            FQ_NAMES.mutableMap.child(Name.identifier("remove")),
            FQ_NAMES.mutableMap.child(Name.identifier("putAll")),
            FQ_NAMES.mutableMap.child(Name.identifier("clear")),
            FQ_NAMES.mutableMap.child(Name.identifier("putIfAbsent")),
            FQ_NAMES.mutableMap.child(Name.identifier("replace")),
            FQ_NAMES.mutableMap.child(Name.identifier("replaceAll")),
            FQ_NAMES.mutableMap.child(Name.identifier("computeIfAbsent")),
            FQ_NAMES.mutableMap.child(Name.identifier("computeIfPresent")),
            FQ_NAMES.mutableMap.child(Name.identifier("compute")),
            FQ_NAMES.mutableMap.child(Name.identifier("merge")),

            FQ_NAMES.mutableMapEntry.child(Name.identifier("setValue")),

            FQ_NAMES.mutableIterator.child(Name.identifier("remove")),

            FQ_NAMES.mutableListIterator.child(Name.identifier("set")),
            FQ_NAMES.mutableListIterator.child(Name.identifier("add"))
        )
    }
}